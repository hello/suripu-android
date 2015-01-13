package is.hello.sense.ui.fragments.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.functions.Action0;

public class DeviceDetailsFragment extends HardwareFragment implements FragmentNavigationActivity.BackInterceptingFragment
{
    public static final int RESULT_UNPAIRED_PILL = 0x66;

    public static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject ApiSessionManager apiSessionManager;
    @Inject DevicesPresenter devicesPresenter;
    @Inject PreferencesPresenter preferences;

    private LinearLayout alertContainer;
    private LinearLayout actionsContainer;

    private Device device;
    private BluetoothAdapter bluetoothAdapter;
    private boolean didEnableBluetooth = false;


    //region Lifecycle

    public static DeviceDetailsFragment newInstance(@NonNull Device device) {
        DeviceDetailsFragment fragment = new DeviceDetailsFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DEVICE, device);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.didEnableBluetooth = savedInstanceState.getBoolean("didEnableBluetooth", false);
        }

        this.device = (Device) getArguments().getSerializable(ARG_DEVICE);
        addPresenter(hardwarePresenter);

        if (device.getType() == Device.Type.SENSE) {
            devicesPresenter.update();
            addPresenter(devicesPresenter);
        }

        this.bluetoothAdapter = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_details, container, false);

        this.alertContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_alert);
        this.actionsContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_actions);

        if (device.getType() == Device.Type.PILL) {
            showSleepPillActions();
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (device.getType() == Device.Type.SENSE) {
            bindAndSubscribe(this.hardwarePresenter.bluetoothEnabled, this::onBluetoothStateChanged, Functions.LOG_ERROR);
            if (bluetoothAdapter.isEnabled()) {
                connectToPeripheral();
            }
        }
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

    private void clearActions() {
        actionsContainer.removeViews(0, actionsContainer.getChildCount());
    }

    private void addDeviceAction(@StringRes int titleRes, boolean wantsDivider, @NonNull View.OnClickListener onClick) {
        actionsContainer.addView(Styles.createItemView(getActivity(), titleRes, R.style.AppTheme_Text_Actionable, onClick));
        if (wantsDivider) {
            actionsContainer.addView(Styles.createHorizontalDivider(getActivity(), ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void showConnectedSenseActions() {
        clearActions();

        addDeviceAction(R.string.action_replace_this_sense, true, ignored -> {});
        addDeviceAction(R.string.action_enter_pairing_mode, true, this::putIntoPairingMode);
        addDeviceAction(R.string.action_factory_reset, true, this::factoryReset);
        addDeviceAction(R.string.action_select_wifi_network, false, this::changeWifiNetwork);
    }

    private void showSleepPillActions() {
        clearActions();

        addDeviceAction(R.string.action_replace_sleep_pill, true, ignored -> {});
    }

    //endregion


    //region Connectivity

    public void onBluetoothStateChanged(boolean isEnabled) {
    }

    public void connectToPeripheral() {
        bindAndSubscribe(this.hardwarePresenter.discoverPeripheralForDevice(device), this::bindPeripheral, this::presentError);
    }

    public void enableBluetooth() {
        showBlockingActivity(R.string.title_turning_on);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(),
                         ignored -> {
                             this.didEnableBluetooth = true;
                             hideBlockingActivity(true, this::connectToPeripheral);
                         },
                         this::presentError);
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
        hideAllActivity(false, () -> {
            if (hardwarePresenter.isErrorFatal(e)) {
                UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
                fragment.show(getFragmentManager(), R.id.activity_fragment_navigation_container);
            } else {
                ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
            }

            Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);
        });
    }


    public void checkConnectivityState() {
        bindAndSubscribe(hardwarePresenter.currentWifiNetwork(),
                         network -> {
                             preferences.edit()
                                        .putString(PreferencesPresenter.PAIRED_DEVICE_SSID, network.ssid)
                                        .apply();

                             showConnectedSenseActions();
                         },
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Could not get connectivity state, ignoring.", e);
                             showConnectedSenseActions();
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
        dialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> {
            showBlockingActivity(R.string.dialog_loading_message);
            showHardwareActivity(() -> {
                bindAndSubscribe(hardwarePresenter.factoryReset(), device -> {
                    Logger.info(getClass().getSimpleName(), "Completed Sense factory reset");
                    Action0 finish = () -> {
                        hideAllActivity(true, () -> {
                            apiSessionManager.logOut();
                            getActivity().finish();
                        });
                    };
                    bindAndSubscribe(devicesPresenter.unregisterAllDevices(),
                            ignored -> {
                                finish.call();
                            },
                            e -> {
                                Logger.error(getClass().getSimpleName(), "Could not unregister all devices, ignoring.", e);
                                finish.call();
                            });
                }, this::presentError);
            });
        });
        dialog.show();
    }

    //endregion


    //region Pill Actions

    public void unpairPill(@NonNull View sender) {
        SenseAlertDialog alertDialog = new SenseAlertDialog(getActivity());
        alertDialog.setDestructive(true);
        alertDialog.setTitle(R.string.dialog_title_unpair_pill);
        alertDialog.setMessage(R.string.dialog_message_unpair_pill);
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.action_unpair, (d, which) -> {
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                             ignored -> finishWithResult(RESULT_UNPAIRED_PILL, null),
                             this::presentError);
        });
        alertDialog.show();
    }

    //endregion
}
