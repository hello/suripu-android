package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

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
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_connection_state;

public class SenseDetailsFragment extends DeviceDetailsFragment implements FragmentNavigationActivity.BackInterceptingFragment {
    private static final int WIFI_REQUEST_CODE = 0x94;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;

    @Inject DevicesPresenter devicesPresenter;
    @Inject PreferencesPresenter preferences;

    private BluetoothAdapter bluetoothAdapter;
    private boolean didEnableBluetooth = false;

    private int discoveryMissCount = 0;

    private final BroadcastReceiver PERIPHERAL_CLEARED = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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

        IntentFilter fatalErrors = new IntentFilter(HardwarePresenter.ACTION_CONNECTION_LOST);
        LocalBroadcastManager.getInstance(getActivity())
                             .registerReceiver(PERIPHERAL_CLEARED, fatalErrors);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindAndSubscribe(this.hardwarePresenter.bluetoothEnabled, this::onBluetoothStateChanged, Functions.LOG_ERROR);
        if (bluetoothAdapter.isEnabled()) {
            connectToPeripheral();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(PERIPHERAL_CLEARED);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("didEnableBluetooth", didEnableBluetooth);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.hardwarePresenter.clearPeripheral();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WIFI_REQUEST_CODE) {
            if (hardwarePresenter.isConnected()) {
                hideAlert();
                checkConnectivityState();
            }
        } else if (requestCode == REQUEST_CODE_HIGH_POWER_RETRY && resultCode == Activity.RESULT_OK) {
            hardwarePresenter.setWantsHighPowerPreScan(true);
            connectToPeripheral();
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

    private void showRestrictedSenseActions() {
        clearActions();
        showActions();

        addDeviceAction(R.string.action_replace_this_sense, true, this::unregisterDevice);
        addDeviceAction(R.string.action_change_time_zone, false, this::changeTimeZone);
    }

    private void showConnectedSenseActions(@Nullable SensePeripheral.SenseWifiNetwork network) {
        clearActions();
        showActions();

        addDeviceAction(R.string.action_replace_this_sense, true, this::unregisterDevice);
        addDeviceAction(R.string.action_enter_pairing_mode, true, this::putIntoPairingMode);
        addDeviceAction(R.string.action_factory_reset, true, this::factoryReset);
        addDeviceAction(R.string.action_select_wifi_network, true, this::changeWifiNetwork);
        addDeviceAction(R.string.action_change_time_zone, false, this::changeTimeZone);

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

                Analytics.trackError(e.getMessage(), e.getClass().getCanonicalName(), null, "Sense Details");
            } else {
                ErrorDialogFragment dialogFragment = ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
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

                             Analytics.trackError(e.getMessage(), e.getClass().getCanonicalName(), "Ignored", "Sense Details");
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
        startActivityForResult(intent, WIFI_REQUEST_CODE);
    }

    public void putIntoPairingMode() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_PUT_INTO_PAIRING_MODE, null);

        showBlockingActivity(R.string.dialog_loading_message);
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                             ignored -> hideAllActivityForSuccess(() -> getFragmentManager().popBackStackImmediate(), this::presentError),
                             this::presentError);
        }, this::presentError);
    }

    public void changeTimeZone() {
        DeviceTimeZoneFragment timeZoneFragment = new DeviceTimeZoneFragment();
        ((FragmentNavigation) getActivity()).pushFragment(timeZoneFragment, getString(R.string.action_change_time_zone), true);
    }

    public void factoryReset() {
        if (!hardwarePresenter.hasPeripheral()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_FACTORY_RESET, null);

        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setDestructive(true);
        dialog.setTitle(R.string.dialog_title_factory_reset);
        dialog.setMessage(R.string.dialog_messsage_factory_reset);
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
        bindAndSubscribe(hardwarePresenter.factoryReset(),
                         device -> {
                             hideBlockingActivity(true, () -> {
                                 Analytics.setSenseId("unpaired");
                                 finishWithResult(RESULT_REPLACED_DEVICE, null);
                             });
                         },
                         this::presentError);
    }

    public void unregisterDevice() {
        Analytics.trackEvent(Analytics.TopView.EVENT_REPLACE_SENSE, null);

        SenseAlertDialog alertDialog = new SenseAlertDialog(getActivity());
        alertDialog.setDestructive(true);
        alertDialog.setTitle(R.string.dialog_title_replace_sense);
        alertDialog.setMessage(R.string.dialog_message_replace_sense);
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.action_replace_device, (d, which) -> {
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                             ignored -> {
                                 Analytics.setSenseId("unpaired");
                                 finishDeviceReplaced();
                             },
                             this::presentError);
        });
        alertDialog.show();
    }

    //endregion
}
