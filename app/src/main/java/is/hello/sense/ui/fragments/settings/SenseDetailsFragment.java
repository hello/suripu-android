package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
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
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.util.Rx;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.SenseNetworkStatus;
import is.hello.commonsense.service.SenseService;
import is.hello.commonsense.service.SenseServiceConnection;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.SensePresenter;
import is.hello.sense.permissions.Permissions;
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
        implements OnBackPressedInterceptor, FragmentCompat.OnRequestPermissionsResultCallback {
    private static final int REQUEST_CODE_WIFI = 0x94;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_ADVANCED = 0xAd;

    private static final int OPTION_ID_REPLACE_SENSE = 0;
    private static final int OPTION_ID_FACTORY_RESET = 1;

    @Inject DevicesPresenter devicesPresenter;
    @Inject AccountPresenter accountPresenter;

    @Inject SensePresenter sensePresenter;
    @Inject SenseServiceConnection serviceConnection;
    @Inject BluetoothStack bluetoothStack;


    private TextView pairingMode;
    private TextView changeWiFi;

    private boolean blockConnection = false;
    private boolean didEnableBluetooth = false;

    private @Nullable SenseNetworkStatus currentWifiNetwork;


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
        addPresenter(sensePresenter);
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

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final IntentFilter disconnectedIntent = new IntentFilter(GattPeripheral.ACTION_DISCONNECTED);
        final Observable<Intent> onDisconnect =
                Rx.fromLocalBroadcast(getActivity(), disconnectedIntent)
                  .filter(i -> !blockConnection && sensePresenter.isDisconnectIntentForSense(i));
        bindAndSubscribe(onDisconnect,
                         ignored -> onPeripheralDisconnected(),
                         Functions.LOG_ERROR);

        bindAndSubscribe(bluetoothStack.enabled(),
                         this::onBluetoothStateChanged,
                         Functions.LOG_ERROR);

        bindAndSubscribe(sensePresenter.peripheral,
                         this::bindPeripheral,
                         this::presentError);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Permissions.needsLocationPermission(this)) {
            showPermissionPrompt();
        } else if (bluetoothStack.isEnabled() && !blockConnection) {
            discoverPeripheralIfNeeded();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.pairingMode = null;
        this.changeWiFi = null;

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        final Observable<SenseService> fadeOutLEDs = serviceConnection.perform(service -> {
            if (service.isConnected()) {
                return service.fadeOutLEDs();
            } else {
                return Observable.just(service);
            }
        });
        fadeOutLEDs.subscribe(Functions.NO_OP, Functions.LOG_ERROR);
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
            bindAndSubscribe(serviceConnection.senseService().filter(SenseService::isConnected),
                             service -> {
                                 hideAlert();
                                 checkConnectivityState(service, true);
                             },
                             Functions.LOG_ERROR);
        } else if (requestCode == REQUEST_CODE_HIGH_POWER_RETRY && resultCode == Activity.RESULT_OK) {
            sensePresenter.setWantsHighPowerPreScan(true);
            discoverPeripheralIfNeeded();
        } else if (requestCode == REQUEST_CODE_ADVANCED && resultCode == Activity.RESULT_OK) {
            final int optionId = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, 0);
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
            final SenseAlertDialog turnOffDialog = new SenseAlertDialog(getActivity());
            turnOffDialog.setTitle(R.string.title_turn_off_bluetooth);
            turnOffDialog.setMessage(R.string.message_turn_off_bluetooth);
            turnOffDialog.setPositiveButton(R.string.action_turn_off, (dialog, which) -> {
                bluetoothStack.turnOff().subscribe(Functions.NO_OP,
                                                   Functions.LOG_ERROR);
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


    //region Permissions

    private void showPermissionPrompt() {
        final TroubleshootingAlert alert = new TroubleshootingAlert()
                .setMessage(StringRef.from(R.string.request_permission_location_message))
                .setPrimaryButtonTitle(R.string.action_enable_location)
                .setPrimaryButtonOnClick(this::promptForLocationPermission)
                .setSecondaryButtonTitle(R.string.action_more_info)
                .setSecondaryButtonOnClick(() -> UserSupport.showLocationPermissionMoreInfoPage(getActivity()));
        showTroubleshootingAlert(alert);
        showRestrictedSenseActions();
    }

    private void promptForLocationPermission() {
        FragmentCompat.requestPermissions(this,
                                          Permissions.getLocationPermissions(),
                                          Permissions.LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Permissions.isLocationPermissionGranted(requestCode, permissions, grantResults)) {
            if (bluetoothStack.isEnabled() && !blockConnection) {
                discoverPeripheralIfNeeded();
            }
        } else {
            showPermissionPrompt();
            Permissions.showEnableInstructionsDialog(this);
        }
    }

    //endregion


    //region Connectivity

    public void onPeripheralDisconnected() {
        LoadingDialogFragment.close(getFragmentManager());

        final TroubleshootingAlert alert = new TroubleshootingAlert()
                .setMessage(StringRef.from(R.string.error_peripheral_connection_lost))
                .setPrimaryButtonTitle(R.string.action_reconnect)
                .setPrimaryButtonOnClick(SenseDetailsFragment.this::discoverPeripheralIfNeeded);
        showTroubleshootingAlert(alert);
        showRestrictedSenseActions();
    }

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

    public void discoverPeripheralIfNeeded() {
        if (sensePresenter.shouldScan()) {
            showBlockingAlert(R.string.info_connecting_to_sense);
            sensePresenter.scanForDevice(device);
        }
    }

    public void enableBluetooth() {
        showBlockingAlert(R.string.title_turning_on);
        bindAndSubscribe(bluetoothStack.turnOn(),
                         ignored -> {
                             this.didEnableBluetooth = true;
                             discoverPeripheralIfNeeded();
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

    public void bindPeripheral(@NonNull GattPeripheral peripheral) {
        bindAndSubscribe(serviceConnection.senseService(), service -> {
            if (service.isConnected()) {
                checkConnectivityState(service, false);
            } else {
                bindAndSubscribe(service.connect(peripheral),
                                 status -> {
                                     if (status == ConnectProgress.CONNECTED) {
                                         sensePresenter.setLastAddress(peripheral.getAddress());
                                         checkConnectivityState(service, false);
                                     }
                                 },
                                 this::presentError);
            }
        }, Functions.LOG_ERROR);
    }

    public void presentError(@NonNull Throwable e) {
        hideAlert();
        LoadingDialogFragment.close(getFragmentManager());
        final Observable<SenseService> stopSenseLEDs = serviceConnection.perform(service -> {
            if (service.isConnected()) {
                return service.stopLEDs();
            } else {
                return Observable.just(service);
            }
        });
        stopSenseLEDs.subscribe(Functions.NO_OP, Functions.LOG_ERROR);

        if (e instanceof SenseNotFoundError) {
            sensePresenter.trackPeripheralNotFound();

            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(R.string.error_sense_not_found))
                    .setPrimaryButtonTitle(R.string.action_troubleshoot)
                    .setPrimaryButtonOnClick(() -> showSupportFor(UserSupport.DeviceIssue.CANNOT_CONNECT_TO_SENSE))
                    .setSecondaryButtonTitle(R.string.action_retry)
                    .setSecondaryButtonOnClick(this::discoverPeripheralIfNeeded);
            showTroubleshootingAlert(alert);

            if (sensePresenter.shouldPromptForHighPowerScan()) {
                final PromptForHighPowerDialogFragment dialogFragment =
                        new PromptForHighPowerDialogFragment();
                dialogFragment.setTargetFragment(this, REQUEST_CODE_HIGH_POWER_RETRY);
                dialogFragment.show(getFragmentManager(), PromptForHighPowerDialogFragment.TAG);
            }

            Analytics.trackError(e, "Sense Details");
        } else {
            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(e, getResources())
                            .withOperation("Sense Details")
                            .withSupportLink();

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }

        showRestrictedSenseActions();

        Logger.error(SenseDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);
    }


    public void checkConnectivityState(@NonNull SenseService service,
                                       boolean ignoreCachedNetwork) {
        if (!ignoreCachedNetwork && currentWifiNetwork != null) {
            showConnectedSenseActions(currentWifiNetwork);
        } else {
            showBlockingAlert(R.string.title_checking_connectivity);
            bindAndSubscribe(service.currentWifiNetwork(),
                             network -> {
                                 this.currentWifiNetwork = network;

                                 showConnectedSenseActions(network);
                             },
                             this::presentError);
        }
    }

    //endregion


    //region Sense Actions

    public void changeWifiNetwork() {
        if (!serviceConnection.isConnectedToSense()) {
            return;
        }

        Analytics.trackEvent(Analytics.Backside.EVENT_EDIT_WIFI, null);

        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity());
        builder.setDefaultTitle(R.string.title_edit_wifi);
        builder.setFragmentClass(SelectWiFiNetworkFragment.class);
        builder.setArguments(SelectWiFiNetworkFragment.createSettingsArguments());
        builder.setWindowBackgroundColor(ContextCompat.getColor(getActivity(), R.color.background_onboarding));
        builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        startActivityForResult(builder.toIntent(), REQUEST_CODE_WIFI);
    }

    public void putIntoPairingMode() {
        if (!serviceConnection.isConnectedToSense()) {
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
            serviceConnection.perform(SenseService::busyLEDs)
                             .subscribe(service -> {
                                 bindAndSubscribe(service.enablePairingMode(),
                                                  ignored -> {
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
        final SenseAlertDialog useCurrentPrompt = new SenseAlertDialog(getActivity());

        useCurrentPrompt.setTitle(R.string.title_use_current_time_zone);

        final String name = DateTimeZone.getDefault().getName(System.currentTimeMillis());
        final SpannableStringBuilder message = new SpannableStringBuilder(name);
        message.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.insert(0, getString(R.string.message_use_current_time_zone));
        useCurrentPrompt.setMessage(message);

        useCurrentPrompt.setPositiveButton(R.string.action_set_this_time_zone, (dialog, which) -> {
            final SenseTimeZone senseTimeZone = SenseTimeZone.fromDateTimeZone(DateTimeZone.getDefault());
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

        final ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_REPLACE_SENSE)
                        .setTitle(R.string.action_replace_this_sense)
                        .setTitleColor(ContextCompat.getColor(getActivity(), R.color.text_dark))
                        .setDescription(R.string.description_replace_this_sense)
                        .setIcon(R.drawable.settings_advanced)
                   );
        if (serviceConnection.isConnectedToSense()) {
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_FACTORY_RESET)
                            .setTitle(R.string.action_factory_reset)
                            .setTitleColor(ContextCompat.getColor(getActivity(), R.color.destructive_accent))
                            .setDescription(R.string.description_factory_reset)
                            .setIcon(R.drawable.settings_factory_reset)
                       );
        }
        final BottomSheetDialogFragment advancedOptions =
                BottomSheetDialogFragment.newInstance(R.string.title_advanced, options);
        advancedOptions.setTargetFragment(this, REQUEST_CODE_ADVANCED);
        advancedOptions.showAllowingStateLoss(getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    public void factoryReset() {
        if (!serviceConnection.isConnectedToSense()) {
            return;
        }

        Analytics.trackEvent(Analytics.Backside.EVENT_FACTORY_RESET, null);

        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        dialog.setTitle(R.string.dialog_title_factory_reset);

        final SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
        message.insert(0, getString(R.string.dialog_message_factory_reset));
        dialog.setMessage(message);

        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> completeFactoryReset());
        dialog.show();
    }

    private void completeFactoryReset() {
        final LoadingDialogFragment loadingDialogFragment =
                LoadingDialogFragment.newInstance(getString(R.string.dialog_loading_message),
                                                  LoadingDialogFragment.OPAQUE_BACKGROUND);
        // Whenever this class gets redone in true MVP style,
        // this can probably be removed by the presenter managing
        // the loading view state. Relevant issue [#97240482].
        loadingDialogFragment.setLockOrientation();
        loadingDialogFragment.showAllowingStateLoss(getFragmentManager(), LoadingDialogFragment.TAG);
        serviceConnection.perform(SenseService::busyLEDs)
                         .subscribe(service -> {
                                        this.blockConnection = true;

                                        final Observable<VoidResponse> factoryReset =
                                                service.factoryReset()
                                                       .flatMap(ignored -> devicesPresenter.removeSenseAssociations(device));
                                        bindAndSubscribe(factoryReset,
                                                         device -> {
                                                             loadingDialogFragment.dismissSafely();
                                                             Analytics.setSenseId("unpaired");

                                                             final MessageDialogFragment powerCycleDialog =
                                                                     MessageDialogFragment.newInstance(R.string.title_power_cycle_sense_factory_reset,
                                                                                                       R.string.message_power_cycle_sense_factory_reset);
                                                             powerCycleDialog.showAllowingStateLoss(getFragmentManager(),
                                                                                                    MessageDialogFragment.TAG);

                                                             finishWithResult(RESULT_REPLACED_DEVICE, null);
                                                         },
                                                         this::presentError);
                                    },
                                    e -> {
                                        stateSafeExecutor.execute(() -> presentError(e));
                                    });
    }

    public void replaceDevice() {
        Analytics.trackEvent(Analytics.Backside.EVENT_REPLACE_SENSE, null);

        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        dialog.setTitle(R.string.dialog_title_replace_sense);

        final SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
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
