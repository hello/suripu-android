package is.hello.sense.ui.fragments.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class SenseDetailsFragment extends DeviceDetailsFragment implements FragmentNavigationActivity.BackInterceptingFragment {
    @Inject DevicesPresenter devicesPresenter;
    @Inject PreferencesPresenter preferences;

    private BluetoothAdapter bluetoothAdapter;
    private boolean didEnableBluetooth = false;


    //region Lifecycle

    public static SenseDetailsFragment newInstance(@NonNull Device device) {
        SenseDetailsFragment fragment = new SenseDetailsFragment();
        fragment.setArguments(getArguments(device));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.didEnableBluetooth = savedInstanceState.getBoolean("didEnableBluetooth", false);
        }

        devicesPresenter.update();
        addPresenter(devicesPresenter);

        this.bluetoothAdapter = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindAndSubscribe(this.hardwarePresenter.bluetoothEnabled, this::onBluetoothStateChanged, Functions.LOG_ERROR);
        if (bluetoothAdapter.isEnabled()) {
            connectToPeripheral();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

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

        addDeviceAction(R.string.action_replace_this_sense, true, this::unpairDevice);
    }

    private void showConnectedSenseActions(@Nullable SensePeripheral.SenseWifiNetwork network) {
        clearActions();
        showActions();

        addDeviceAction(R.string.action_replace_this_sense, true, this::unpairDevice);
        addDeviceAction(R.string.action_enter_pairing_mode, true, this::putIntoPairingMode);
        addDeviceAction(R.string.action_factory_reset, true, this::factoryReset);
        addDeviceAction(R.string.action_select_wifi_network, false, this::changeWifiNetwork);

        if (network == null || TextUtils.isEmpty(network.ssid)) {
            showTroubleshootingAlert(R.string.error_sense_no_connectivity, R.string.action_troubleshoot, ignored -> UserSupport.showForDeviceIssue(getActivity(), UserSupport.DeviceIssue.WIFI_CONNECTIVITY));
        } else if (device.isMissing()) {
            String missingMessage = getString(R.string.error_sense_missing_fmt, device.getLastUpdatedDescription(getActivity()));
            showTroubleshootingAlert(missingMessage, R.string.action_troubleshoot, ignored -> UserSupport.showForDeviceIssue(getActivity(), UserSupport.DeviceIssue.SENSE_MISSING));
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

    public void enableBluetooth(@NonNull View sender) {
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

    public void bindPeripheral(@NonNull SensePeripheral peripheral) {
        if (peripheral.isConnected()) {
            checkConnectivityState();
        } else {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(peripheral),
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
        hideAllActivity(false, () -> {
            if (hardwarePresenter.isErrorFatal(e)) {
                UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
                fragment.show(getFragmentManager(), R.id.activity_fragment_navigation_container);
            } else if (e instanceof PeripheralNotFoundError) {
                showTroubleshootingAlert(R.string.error_sense_not_found, R.string.action_retry, ignored -> connectToPeripheral());
            } else {
                ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
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
                         });
    }

    //endregion


    //region Sense Actions

    public void changeWifiNetwork(@NonNull View sender) {
        if (hardwarePresenter.getPeripheral() == null)
            return;

        Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, true);
        startActivity(intent);
    }

    public void putIntoPairingMode(@NonNull View sender) {
        Analytics.trackEvent(Analytics.EVENT_DEVICE_ACTION, Analytics.createProperties(Analytics.PROP_DEVICE_ACTION, Analytics.PROP_DEVICE_ACTION_ENABLE_PAIRING_MODE));

        if (hardwarePresenter.getPeripheral() == null)
            return;

        showBlockingActivity(R.string.dialog_loading_message);
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                             ignored -> hideAllActivity(true, () -> getFragmentManager().popBackStackImmediate()),
                             this::presentError);
        });
    }

    @SuppressWarnings("CodeBlock2Expr")
    public void factoryReset(@NonNull View sender) {
        Analytics.trackEvent(Analytics.EVENT_DEVICE_ACTION, Analytics.createProperties(Analytics.PROP_DEVICE_ACTION, Analytics.PROP_DEVICE_ACTION_FACTORY_RESTORE));

        if (hardwarePresenter.getPeripheral() == null) {
            return;
        }

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
            bindAndSubscribe(devicesPresenter.unregisterAllDevices(),
                             ignored -> completeFactoryReset(),
                             this::presentError);
        });
    }

    private void completeFactoryReset() {
        bindAndSubscribe(hardwarePresenter.factoryReset(),
                         device -> {
                             hideBlockingActivity(true, () -> finishWithResult(RESULT_REPLACED_DEVICE, null));
                         },
                         this::presentError);
    }

    public void unpairDevice(@NonNull View sender) {
        SenseAlertDialog alertDialog = new SenseAlertDialog(getActivity());
        alertDialog.setDestructive(true);
        alertDialog.setTitle(R.string.dialog_title_replace_sense);
        alertDialog.setMessage(R.string.dialog_message_replace_sense);

        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.action_replace_device, (d, which) -> {
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                             ignored -> finishDeviceReplaced(),
                             this::presentError);
        });
        alertDialog.show();
    }

    //endregion
}
