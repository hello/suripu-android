package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.segment.analytics.Properties;

import org.joda.time.DateTimeZone;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.commonsense.bluetooth.model.SenseNetworkStatus;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_connection_state;

public class SenseDetailsFragment extends DeviceDetailsFragment<SenseDevice>
        implements OnBackPressedInterceptor {
    private static final int REQUEST_CODE_WIFI = 0x94;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_ADVANCED = 0xAd;

    private static final int OPTION_ID_REPLACE_SENSE = 0;
    private static final int OPTION_ID_FACTORY_RESET = 1;

    @Inject DevicesPresenter devicesPresenter;
    @Inject HardwarePresenter hardwarePresenter;
    @Inject AccountPresenter accountPresenter;
    @Inject BluetoothStack bluetoothStack;

    private TextView pairingMode;
    private TextView changeWiFi;

    private boolean blockConnection = false;
    private boolean didEnableBluetooth = false;

    private @Nullable SenseNetworkStatus currentWifiNetwork;

    private final BroadcastReceiver PERIPHERAL_CLEARED = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (blockConnection) {
                return;
            }

            stateSafeExecutor.execute(() -> LoadingDialogFragment.close(getFragmentManager()));
            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(R.string.error_peripheral_connection_lost))
                    .setPrimaryButtonTitle(R.string.action_reconnect)
                    .setPrimaryButtonOnClick(SenseDetailsFragment.this::connectToPeripheral);
            showTroubleshootingAlert(alert);
            showRestrictedSenseActions();
        }
    };


    //region Lifecycle

    public static SenseDetailsFragment newInstance(@NonNull SenseDevice device) {
        final SenseDetailsFragment fragment = new SenseDetailsFragment();
        fragment.setArguments(createArguments(device));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.didEnableBluetooth = savedInstanceState.getBoolean("didEnableBluetooth", false);
            this.blockConnection = savedInstanceState.getBoolean("blockConnection", false);
        } else {
            Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
            Analytics.trackEvent(Analytics.Backside.EVENT_SENSE_DETAIL, properties);
        }

        devicesPresenter.update();
        addPresenter(devicesPresenter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.pairingMode = addDeviceAction(R.drawable.icon_settings_pairing_mode, R.string.action_enter_pairing_mode, this::putIntoPairingMode);
        setEnabled(pairingMode, false);
        this.changeWiFi = addDeviceAction(R.drawable.icon_settings_wifi, R.string.action_select_wifi_network, this::changeWifiNetwork);
        setEnabled(changeWiFi, false);
        addDeviceAction(R.drawable.icon_settings_timezone, R.string.action_change_time_zone, this::changeTimeZone);
        addDeviceAction(R.drawable.icon_settings_advanced, R.string.title_advanced, this::showAdvancedOptions);
        showActions();

        IntentFilter fatalErrors = new IntentFilter(HardwarePresenter.ACTION_CONNECTION_LOST);
        LocalBroadcastManager.getInstance(getActivity())
                             .registerReceiver(PERIPHERAL_CLEARED, fatalErrors);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindAndSubscribe(this.hardwarePresenter.bluetoothEnabled, this::onBluetoothStateChanged, Functions.LOG_ERROR);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (bluetoothStack.isEnabled() && !blockConnection) {
            connectToPeripheral();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.pairingMode = null;
        this.changeWiFi = null;

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(PERIPHERAL_CLEARED);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("didEnableBluetooth", didEnableBluetooth);
        outState.putBoolean("blockConnection", blockConnection);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIFI) {
            if (hardwarePresenter.isConnected()) {
                hideAlert();
                checkConnectivityState(true);
            }
        } else if (requestCode == REQUEST_CODE_HIGH_POWER_RETRY && resultCode == Activity.RESULT_OK) {
            hardwarePresenter.setWantsHighPowerPreScan(true);
            connectToPeripheral();
        } else if (requestCode == REQUEST_CODE_ADVANCED && resultCode == Activity.RESULT_OK) {
            int optionId = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, 0);
            switch (optionId) {
                case OPTION_ID_REPLACE_SENSE: {
                    replaceDevice();
                    break;
                }

                case OPTION_ID_FACTORY_RESET: {
                    factoryReset();
                    break;
                }

                default: {
                    Logger.warn(getClass().getSimpleName(), "Unknown option " + optionId);
                    break;
                }
            }
        }
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull Runnable defaultBehavior) {
        if (didEnableBluetooth) {
            SenseAlertDialog turnOffDialog = new SenseAlertDialog(getActivity());
            turnOffDialog.setTitle(R.string.title_turn_off_bluetooth);
            turnOffDialog.setMessage(R.string.message_turn_off_bluetooth);
            turnOffDialog.setPositiveButton(R.string.action_turn_off, (dialog, which) -> {
                hardwarePresenter.turnOffBluetooth().subscribe(Functions.NO_OP, Functions.LOG_ERROR);
                defaultBehavior.run();
            });
            turnOffDialog.setNegativeButton(R.string.action_no_thanks, (dialog, which) -> defaultBehavior.run());
            turnOffDialog.show();

            return true;
        }
        return false;
    }

    //endregion


    //region Displaying Actions

    private void setEnabled(@NonNull TextView item, boolean enabled) {
        final Drawable drawable = item.getCompoundDrawablesRelative()[0];
        drawable.setAlpha(enabled ? 255 : 0x45);
        item.setEnabled(enabled);
    }

    @Override
    protected void clearActions() {
        setEnabled(pairingMode, false);
        setEnabled(changeWiFi, false);
    }

    private void showRestrictedSenseActions() {
        clearActions();
    }

    private void showConnectedSenseActions(@Nullable SenseNetworkStatus network) {
        setEnabled(pairingMode, true);
        setEnabled(changeWiFi, true);

        if (network == null ||
                TextUtils.isEmpty(network.ssid) ||
                wifi_connection_state.IP_RETRIEVED != network.connectionState) {
            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(R.string.error_sense_no_connectivity))
                    .setPrimaryButtonTitle(R.string.action_troubleshoot)
                    .setPrimaryButtonOnClick(() -> showSupportFor(UserSupport.DeviceIssue.SENSE_NO_WIFI));
            showTroubleshootingAlert(alert);
        } else if (device.isMissing()) {
            final String missingMessage = getString(R.string.error_sense_missing_fmt,
                                                    device.getLastUpdatedDescription(getActivity()));
            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(missingMessage))
                    .setPrimaryButtonTitle(R.string.action_troubleshoot)
                    .setPrimaryButtonOnClick(() -> showSupportFor(UserSupport.DeviceIssue.SENSE_MISSING));
            showTroubleshootingAlert(alert);
        } else {
            hideAlert();
        }
    }

    //endregion


    //region Connectivity

    public void onBluetoothStateChanged(boolean isEnabled) {
        if (!isEnabled) {
            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(R.string.error_no_bluetooth_connectivity))
                    .setPrimaryButtonTitle(R.string.action_turn_on_ble)
                    .setPrimaryButtonOnClick(this::enableBluetooth);
            showTroubleshootingAlert(alert);
            showRestrictedSenseActions();
        }
    }

    public void connectToPeripheral() {
        showBlockingAlert(R.string.info_connecting_to_sense);
        bindAndSubscribe(this.hardwarePresenter.discoverPeripheralForDevice(device),
                         this::bindPeripheral,
                         this::presentError);
    }

    public void enableBluetooth() {
        showBlockingAlert(R.string.title_turning_on);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(),
                         ignored -> {
                             this.didEnableBluetooth = true;
                             connectToPeripheral();
                         },
                         e -> {
                             final TroubleshootingAlert alert = new TroubleshootingAlert()
                                     .setMessage(StringRef.from(R.string.error_no_bluetooth_connectivity))
                                     .setPrimaryButtonTitle(R.string.action_turn_on_ble)
                                     .setPrimaryButtonOnClick(this::enableBluetooth);
                             showTroubleshootingAlert(alert);
                             presentError(e);
                         });
    }

    public void bindPeripheral(@NonNull SensePeripheral ignored) {
        if (hardwarePresenter.isConnected()) {
            checkConnectivityState(false);
        } else {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(),
                             status -> {
                                 if (status == ConnectProgress.CONNECTED) {
                                     checkConnectivityState(false);
                                 }
                             },
                             this::presentError);
        }
    }

    public void presentError(@NonNull Throwable e) {
        hideAlert();
        LoadingDialogFragment.close(getFragmentManager());
        runLedAnimation(SenseLedAnimation.STOP).subscribe(Functions.NO_OP, Functions.LOG_ERROR);

        if (e instanceof SenseNotFoundError) {
            hardwarePresenter.trackPeripheralNotFound();

            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(R.string.error_sense_not_found))
                    .setPrimaryButtonTitle(R.string.action_troubleshoot)
                    .setPrimaryButtonOnClick(() -> showSupportFor(UserSupport.DeviceIssue.CANNOT_CONNECT_TO_SENSE))
                    .setSecondaryButtonTitle(R.string.action_retry)
                    .setSecondaryButtonOnClick(this::connectToPeripheral);
            showTroubleshootingAlert(alert);

            if (hardwarePresenter.shouldPromptForHighPowerScan()) {
                PromptForHighPowerDialogFragment dialogFragment = new PromptForHighPowerDialogFragment();
                dialogFragment.setTargetFragment(this, REQUEST_CODE_HIGH_POWER_RETRY);
                dialogFragment.show(getFragmentManager(), PromptForHighPowerDialogFragment.TAG);
            }

            Analytics.trackError(e, "Sense Details");
        } else {
            ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getResources())
                    .withOperation("Sense Details")
                    .withSupportLink();

            ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }

        showRestrictedSenseActions();

        Logger.error(SenseDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);
    }


    public void checkConnectivityState(boolean ignoreCachedNetwork) {
        if (!ignoreCachedNetwork && currentWifiNetwork != null) {
            showConnectedSenseActions(currentWifiNetwork);
        } else {
            showBlockingAlert(R.string.title_checking_connectivity);
            bindAndSubscribe(hardwarePresenter.currentWifiNetwork(),
                             network -> {
                                 this.currentWifiNetwork = network;

                                 showConnectedSenseActions(network);
                             },
                             this::presentError);
        }
    }

    //endregion


    //region Sense Actions

    private Observable<Void> runLedAnimation(@NonNull SenseLedAnimation animation) {
        if (hardwarePresenter.isConnected()) {
            return hardwarePresenter.runLedAnimation(animation);
        } else {
            return Observable.just(null);
        }
    }

    public void changeWifiNetwork() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.Backside.EVENT_EDIT_WIFI, null);

        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity());
        builder.setDefaultTitle(R.string.title_edit_wifi);
        builder.setFragmentClass(SelectWiFiNetworkFragment.class);
        builder.setArguments(SelectWiFiNetworkFragment.createSettingsArguments());
        builder.setWindowBackgroundColor(getResources().getColor(R.color.background_onboarding));
        builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        startActivityForResult(builder.toIntent(), REQUEST_CODE_WIFI);
    }

    public void putIntoPairingMode() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.Backside.EVENT_PUT_INTO_PAIRING_MODE, null);

        final SenseAlertDialog confirmation = new SenseAlertDialog(getActivity());
        confirmation.setTitle(R.string.dialog_title_put_into_pairing_mode);
        confirmation.setMessage(Styles.resolveSupportLinks(getActivity(),
                                                           getText(R.string.dialog_message_put_into_pairing_mode)));
        confirmation.setNegativeButton(android.R.string.cancel, null);
        confirmation.setPositiveButton(R.string.action_enter_pairing_mode, (dialog, which) -> {
            this.blockConnection = true;

            LoadingDialogFragment.show(getFragmentManager(),
                                       getString(R.string.dialog_loading_message),
                                       LoadingDialogFragment.OPAQUE_BACKGROUND);
            hardwarePresenter.runLedAnimation(SenseLedAnimation.BUSY)
                             .subscribe(ignored -> {
                                 bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                                                  ignored1 -> {
                                                      LoadingDialogFragment.close(getFragmentManager());
                                                      getFragmentManager().popBackStackImmediate();
                                                  },
                                                  this::presentError);
                             }, e -> {
                                 stateSafeExecutor.execute(() -> presentError(e));
                             });
        });
        confirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmation.show();
    }

    public void changeTimeZone() {
        SenseAlertDialog useCurrentPrompt = new SenseAlertDialog(getActivity());

        useCurrentPrompt.setTitle(R.string.title_use_current_time_zone);

        String name = DateTimeZone.getDefault().getName(System.currentTimeMillis());
        SpannableStringBuilder message = new SpannableStringBuilder(name);
        message.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.insert(0, getString(R.string.message_use_current_time_zone));
        useCurrentPrompt.setMessage(message);

        useCurrentPrompt.setPositiveButton(R.string.action_set_this_time_zone, (dialog, which) -> {
            SenseTimeZone senseTimeZone = SenseTimeZone.fromDateTimeZone(DateTimeZone.getDefault());
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.OPAQUE_BACKGROUND);
            bindAndSubscribe(accountPresenter.updateTimeZone(senseTimeZone),
                             ignored -> {
                                 Logger.info(getClass().getSimpleName(), "Updated time zone");

                                 Properties properties = Analytics.createProperties(
                                         Analytics.Backside.PROP_TIME_ZONE, senseTimeZone.timeZoneId
                                                                                   );
                                 Analytics.trackEvent(Analytics.Backside.EVENT_TIME_ZONE_CHANGED, properties);

                                 LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
                             },
                             this::presentError);
        });
        useCurrentPrompt.setNegativeButton(R.string.action_select_time_zone_from_list, (dialog, which) -> {
            final DeviceTimeZoneFragment timeZoneFragment = new DeviceTimeZoneFragment();
            getFragmentNavigation().pushFragment(timeZoneFragment,
                                                 getString(R.string.action_change_time_zone), true);
        });
        useCurrentPrompt.setButtonDeemphasized(DialogInterface.BUTTON_NEGATIVE, true);

        useCurrentPrompt.show();
    }

    public void showAdvancedOptions() {
        Analytics.trackEvent(Analytics.Backside.EVENT_SENSE_ADVANCED, null);

        ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_REPLACE_SENSE)
                        .setTitle(R.string.action_replace_this_sense)
                        .setTitleColor(getResources().getColor(R.color.text_dark))
                        .setDescription(R.string.description_replace_this_sense)
                        .setIcon(R.drawable.settings_advanced)
                   );
        if (hardwarePresenter.isConnected()) {
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_FACTORY_RESET)
                            .setTitle(R.string.action_factory_reset)
                            .setTitleColor(getResources().getColor(R.color.destructive_accent))
                            .setDescription(R.string.description_factory_reset)
                            .setIcon(R.drawable.settings_factory_reset)
                       );
        }
        BottomSheetDialogFragment advancedOptions = BottomSheetDialogFragment.newInstance(R.string.title_advanced, options);
        advancedOptions.setTargetFragment(this, REQUEST_CODE_ADVANCED);
        advancedOptions.showAllowingStateLoss(getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    public void factoryReset() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.Backside.EVENT_FACTORY_RESET, null);

        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        dialog.setTitle(R.string.dialog_title_factory_reset);

        SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
        message.insert(0, getString(R.string.dialog_message_factory_reset));
        dialog.setMessage(message);

        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> completeFactoryReset());
        dialog.show();
    }

    private void completeFactoryReset() {
        LoadingDialogFragment loadingDialogFragment = LoadingDialogFragment.newInstance(getString(R.string.dialog_loading_message),
                                                                                        LoadingDialogFragment.OPAQUE_BACKGROUND);
        // Whenever this class gets redone in true MVP style,
        // this can probably be removed by the presenter managing
        // the loading view state. Relevant issue [#97240482].
        loadingDialogFragment.setLockOrientation();
        loadingDialogFragment.showAllowingStateLoss(getFragmentManager(), LoadingDialogFragment.TAG);
        runLedAnimation(SenseLedAnimation.BUSY).subscribe(ignored -> {
            this.blockConnection = true;

            bindAndSubscribe(hardwarePresenter.factoryReset(device),
                             device -> {
                                 loadingDialogFragment.dismissSafely();
                                 Analytics.setSenseId("unpaired");

                                 MessageDialogFragment powerCycleDialog = MessageDialogFragment.newInstance(R.string.title_power_cycle_sense_factory_reset,
                                                                                                            R.string.message_power_cycle_sense_factory_reset);
                                 powerCycleDialog.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);

                                 finishWithResult(RESULT_REPLACED_DEVICE, null);
                             },
                             this::presentError);
        }, e -> {
            stateSafeExecutor.execute(() -> presentError(e));
        });
    }

    public void replaceDevice() {
        Analytics.trackEvent(Analytics.Backside.EVENT_REPLACE_SENSE, null);

        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        dialog.setTitle(R.string.dialog_title_replace_sense);

        SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
        message.insert(0, getString(R.string.dialog_message_replace_sense));
        dialog.setMessage(message);

        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(R.string.action_replace_device, (d, which) -> {
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                             ignored -> {
                                 Analytics.setSenseId("unpaired");
                                 finishDeviceReplaced();
                             },
                             this::presentError);
        });
        dialog.show();
    }

    //endregion
}
