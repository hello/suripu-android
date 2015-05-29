package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_connection_state;

public class SenseDetailsFragment extends DeviceDetailsFragment implements FragmentNavigationActivity.BackInterceptingFragment {
    private static final int REQUEST_CODE_WIFI = 0x94;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_ADVANCED = 0xAd;

    private static final int OPTION_ID_REPLACE_SENSE = 0;
    private static final int OPTION_ID_FACTORY_RESET = 1;

    @Inject DevicesPresenter devicesPresenter;
    @Inject PreferencesPresenter preferences;

    private View pairingMode;
    private View changeWiFi;

    private BluetoothAdapter bluetoothAdapter;
    private boolean factoryResetting = false;
    private boolean didEnableBluetooth = false;

    private final BroadcastReceiver PERIPHERAL_CLEARED = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (factoryResetting) {
                return;
            }

            hideBlockingActivity(false, () -> {
                showTroubleshootingAlert(R.string.error_peripheral_connection_lost,
                        R.string.action_reconnect,
                        SenseDetailsFragment.this::connectToPeripheral);
                showRestrictedSenseActions();
            });
        }
    };


    //region Lifecycle

    public static SenseDetailsFragment newInstance(@NonNull Device device) {
        SenseDetailsFragment fragment = new SenseDetailsFragment();
        fragment.setArguments(createArguments(device));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.didEnableBluetooth = savedInstanceState.getBoolean("didEnableBluetooth", false);
            this.factoryResetting = savedInstanceState.getBoolean("factoryResetting", false);
        } else {
            Analytics.trackEvent(Analytics.TopView.EVENT_SENSE_DETAIL, null);
        }

        devicesPresenter.update();
        addPresenter(devicesPresenter);

        this.bluetoothAdapter = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.pairingMode = addDeviceAction(R.string.action_enter_pairing_mode, true, this::putIntoPairingMode);
        pairingMode.setEnabled(false);
        this.changeWiFi = addDeviceAction(R.string.action_select_wifi_network, true, this::changeWifiNetwork);
        changeWiFi.setEnabled(false);
        addDeviceAction(R.string.action_change_time_zone, true, this::changeTimeZone);
        addDeviceAction(R.string.title_advanced, false, this::showAdvancedOptions);
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

        if (bluetoothAdapter.isEnabled()) {
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
        outState.putBoolean("factoryResetting", factoryResetting);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIFI) {
            if (hardwarePresenter.isConnected()) {
                hideAlert();
                checkConnectivityState();
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
    public boolean onInterceptBack(@NonNull Runnable back) {
        if (didEnableBluetooth) {
            SenseAlertDialog turnOffDialog = new SenseAlertDialog(getActivity());
            turnOffDialog.setTitle(R.string.title_turn_off_bluetooth);
            turnOffDialog.setMessage(R.string.message_turn_off_bluetooth);
            turnOffDialog.setPositiveButton(R.string.action_turn_off, (dialog, which) -> {
                hardwarePresenter.turnOffBluetooth().subscribe(Functions.NO_OP, Functions.LOG_ERROR);
                back.run();
            });
            turnOffDialog.setNegativeButton(R.string.action_no_thanks, (dialog, which) -> back.run());
            turnOffDialog.show();

            return true;
        }
        return false;
    }

    //endregion


    //region Displaying Actions


    @Override
    protected void clearActions() {
        pairingMode.setEnabled(false);
        changeWiFi.setEnabled(false);
    }

    private void showRestrictedSenseActions() {
        clearActions();
    }

    private void showConnectedSenseActions(@Nullable SensePeripheral.SenseWifiNetwork network) {
        pairingMode.setEnabled(true);
        changeWiFi.setEnabled(true);

        if (network == null ||
            TextUtils.isEmpty(network.ssid) ||
            wifi_connection_state.IP_RETRIEVED != network.connectionState) {
            showTroubleshootingAlert(R.string.error_sense_no_connectivity,
                                     R.string.action_troubleshoot,
                                     () -> showSupportFor(UserSupport.DeviceIssue.SENSE_NO_WIFI));
        } else if (device.isMissing()) {
            String missingMessage = getString(R.string.error_sense_missing_fmt, device.getLastUpdatedDescription(getActivity()));
            showTroubleshootingAlert(missingMessage, R.string.action_troubleshoot, () -> showSupportFor(UserSupport.DeviceIssue.SENSE_MISSING));
        } else {
            hideAlert();
        }
    }

    //endregion


    //region Connectivity

    public void onBluetoothStateChanged(boolean isEnabled) {
        if (!isEnabled) {
            showTroubleshootingAlert(R.string.error_no_bluetooth_connectivity, R.string.action_turn_on_ble, this::enableBluetooth);
            showRestrictedSenseActions();
        }
    }

    public void connectToPeripheral() {
        showBlockingAlert(R.string.info_connecting_to_sense);
        bindAndSubscribe(this.hardwarePresenter.discoverPeripheralForDevice(device), this::bindPeripheral, this::presentError);
    }

    public void enableBluetooth() {
        showBlockingAlert(R.string.title_turning_on);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(),
                         ignored -> {
                             this.didEnableBluetooth = true;
                             connectToPeripheral();
                         },
                         e -> {
                             showTroubleshootingAlert(R.string.error_no_bluetooth_connectivity, R.string.action_turn_on_ble, this::enableBluetooth);
                             presentError(e);
                         });
    }

    public void bindPeripheral(@NonNull SensePeripheral ignored) {
        if (hardwarePresenter.isConnected()) {
            checkConnectivityState();
        } else {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(),
                             status -> {
                                 if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                                     checkConnectivityState();
                                 }
                             },
                             this::presentError);
        }
    }

    public void presentError(@NonNull Throwable e) {
        hideAlert();
        hideAllActivityForFailure(() -> {
            if (e instanceof PeripheralNotFoundError) {
                hardwarePresenter.trackPeripheralNotFound();

                showTroubleshootingAlert(R.string.error_sense_not_found, R.string.action_troubleshoot, () -> showSupportFor(UserSupport.DeviceIssue.CANNOT_CONNECT_TO_SENSE));

                if (hardwarePresenter.shouldPromptForHighPowerScan()) {
                    PromptForHighPowerDialogFragment dialogFragment = new PromptForHighPowerDialogFragment();
                    dialogFragment.setTargetFragment(this, REQUEST_CODE_HIGH_POWER_RETRY);
                    dialogFragment.show(getFragmentManager(), PromptForHighPowerDialogFragment.TAG);
                }

                Analytics.trackError(e, "Sense Details");
            } else {
                ErrorDialogFragment dialogFragment = ErrorDialogFragment.presentBluetoothError(getFragmentManager(), e);
                dialogFragment.setErrorOperation("Sense Details");
            }

            showRestrictedSenseActions();

            Logger.error(SenseDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);
        });
    }


    public void checkConnectivityState() {
        bindAndSubscribe(hardwarePresenter.currentWifiNetwork(),
                         network -> {
                             preferences.edit()
                                        .putString(PreferencesPresenter.PAIRED_DEVICE_SSID, network.ssid)
                                        .apply();

                             showConnectedSenseActions(network);
                         },
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Could not get connectivity state, ignoring.", e);
                             showConnectedSenseActions(null);

                             Analytics.trackError(e, "Sense Details");
                         });
    }

    //endregion


    //region Sense Actions

    public void changeWifiNetwork() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_EDIT_WIFI, null);

        Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, true);
        startActivityForResult(intent, REQUEST_CODE_WIFI);
    }

    public void putIntoPairingMode() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_PUT_INTO_PAIRING_MODE, null);

        showBlockingActivity(R.string.dialog_loading_message);
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                    ignored -> hideBlockingActivity(true, () -> getFragmentManager().popBackStackImmediate()),
                    this::presentError);
        }, this::presentError);
    }

    public void changeTimeZone() {
        DeviceTimeZoneFragment timeZoneFragment = new DeviceTimeZoneFragment();
        ((FragmentNavigation) getActivity()).pushFragment(timeZoneFragment, getString(R.string.action_change_time_zone), true);
    }

    public void showAdvancedOptions() {
        Analytics.trackEvent(Analytics.TopView.EVENT_SENSE_ADVANCED, null);

        ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_REPLACE_SENSE)
                        .setTitle(R.string.action_replace_this_sense)
                        .setTitleColor(getResources().getColor(R.color.light_accent))
                        .setDescription(R.string.description_replace_this_sense)
        );
        if (hardwarePresenter.isConnected()) {
            options.add(
                new SenseBottomSheet.Option(OPTION_ID_FACTORY_RESET)
                        .setTitle(R.string.action_factory_reset)
                        .setTitleColor(getResources().getColor(R.color.destructive_accent))
                        .setDescription(R.string.description_factory_reset)
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

        Analytics.trackEvent(Analytics.TopView.EVENT_FACTORY_RESET, null);

        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        dialog.setTitle(R.string.dialog_title_factory_reset);

        SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
        message.insert(0, getString(R.string.dialog_message_factory_reset));
        dialog.setMessage(message);

        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> resetAllDevices());
        dialog.show();
    }

    private void resetAllDevices() {
        showBlockingActivity(R.string.dialog_loading_message);
        showHardwareActivity(() -> {
            bindAndSubscribe(devicesPresenter.removeSenseAssociations(device),
                             ignored -> completeFactoryReset(),
                             this::presentError);
        }, this::presentError);
    }

    private void completeFactoryReset() {
        this.factoryResetting = true;

        bindAndSubscribe(hardwarePresenter.factoryReset(),
                         device -> {
                             hideBlockingActivity(true, () -> {
                                 Analytics.setSenseId("unpaired");

                                 MessageDialogFragment powerCycleDialog = MessageDialogFragment.newInstance(R.string.title_power_cycle_sense_factory_reset, R.string.message_power_cycle_sense_factory_reset);
                                 powerCycleDialog.show(getFragmentManager(), MessageDialogFragment.TAG);

                                 finishWithResult(RESULT_REPLACED_DEVICE, null);
                             });
                         },
                         this::presentError);
    }

    public void replaceDevice() {
        Analytics.trackEvent(Analytics.TopView.EVENT_REPLACE_SENSE, null);

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
