package is.hello.sense.ui.fragments.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.functions.Action0;

public class DeviceDetailsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    public static final int RESULT_UNPAIRED_PILL = 0x66;

    public static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject ApiSessionManager apiSessionManager;
    @Inject DateFormatter dateFormatter;
    @Inject HardwarePresenter hardwarePresenter;
    @Inject DevicesPresenter devicesPresenter;

    private ProgressBar activityIndicator;
    private ViewGroup senseActionsContainer;
    private @Nullable SensorStateView signalStrength;
    private Button actionButton;

    private Device device;
    private BluetoothAdapter bluetoothAdapter;

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

        SensorStateView lastSeen = (SensorStateView) view.findViewById(R.id.fragment_device_details_last_seen);
        lastSeen.setReading(dateFormatter.formatAsDate(device.getLastUpdated()));

        SensorStateView version = (SensorStateView) view.findViewById(R.id.fragment_device_details_version);
        version.setReading(device.getFirmwareVersion());

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_device_details_activity);

        if (device.getType() == Device.Type.SENSE) {
            this.senseActionsContainer = (ViewGroup) view.findViewById(R.id.fragment_device_details_sense);
            this.signalStrength = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_signal);

            SensorStateView changeWifi = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_change_wifi);
            Views.setSafeOnClickListener(changeWifi, this::changeWifiNetwork);

            SensorStateView enterPairingMode = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_pairing_mode);
            Views.setSafeOnClickListener(enterPairingMode, this::putIntoPairingMode);

            SensorStateView pairNewPill = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_pair_new_pill);
            Views.setSafeOnClickListener(pairNewPill, this::pairNewPill);

            SensorStateView factoryReset = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_factory_reset);
            Views.setSafeOnClickListener(factoryReset, this::factoryReset);

            this.actionButton = (Button) view.findViewById(R.id.fragment_device_details_action);
            Views.setSafeOnClickListener(actionButton, ignored -> {
                if (bluetoothAdapter.isEnabled()) {
                    connectToPeripheral();
                } else {
                    enableBluetooth();
                }
            });
        } else if (device.getType() == Device.Type.PILL) {
            LinearLayout pillActionsContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_pill);
            pillActionsContainer.setVisibility(View.VISIBLE);

            SensorStateView unpairPill = (SensorStateView) pillActionsContainer.findViewById(R.id.fragment_device_details_pill_unpair);
            Views.setSafeOnClickListener(unpairPill, this::unpairPill);
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
    public void onDestroy() {
        super.onDestroy();

        this.hardwarePresenter.clearPeripheral();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        StaticItemAdapter.Item item = (StaticItemAdapter.Item) adapterView.getItemAtPosition(position);
        if (item.getAction() != null)
            item.getAction().run();
    }


    public void onBluetoothStateChanged(boolean isEnabled) {
        if (isEnabled) {
            actionButton.setText(R.string.action_retry);
        } else {
            if (signalStrength != null) {
                signalStrength.setReading(getString(R.string.missing_data_placeholder));
                senseActionsContainer.setVisibility(View.GONE);
                activityIndicator.setVisibility(View.GONE);
            }

            actionButton.setVisibility(View.VISIBLE);
            actionButton.setText(R.string.action_turn_on_ble);
        }
    }

    public void connectToPeripheral() {
        activityIndicator.setVisibility(View.VISIBLE);
        senseActionsContainer.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);

        bindAndSubscribe(this.hardwarePresenter.discoverPeripheralForDevice(device), this::bindPeripheral, this::presentError);
    }

    public void enableBluetooth() {
        LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_turning_on), true);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             connectToPeripheral();
                         },
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             presentError(e);
                         });
    }

    public void bindPeripheral(@NonNull SensePeripheral peripheral) {
        int rssi = peripheral.getScannedRssi();
        String strength;
        if (rssi <= -30) {
            strength = getString(R.string.signal_strong);
        } else if (rssi <= -50) {
            strength = getString(R.string.signal_good);
        } else {
            strength = getString(R.string.signal_weak);
        }

        if (signalStrength != null) {
            signalStrength.setReading(strength);
        }

        if (peripheral.isConnected()) {
            senseActionsContainer.setVisibility(View.VISIBLE);
            activityIndicator.setVisibility(View.GONE);
        } else {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(peripheral),
                             status -> {
                                 if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                                     senseActionsContainer.setVisibility(View.VISIBLE);
                                     activityIndicator.setVisibility(View.GONE);
                                 }
                             },
                             this::presentError);
        }
    }

    public void presentError(@NonNull Throwable e) {
        activityIndicator.setVisibility(View.GONE);
        LoadingDialogFragment.close(getFragmentManager());

        if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_fragment_navigation_container);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }

        Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);

        if (signalStrength != null) {
            signalStrength.setReading(getString(R.string.missing_data_placeholder));
            senseActionsContainer.setVisibility(View.GONE);
        }

        if (actionButton != null) {
            actionButton.setVisibility(View.VISIBLE);
        }
    }


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

        LoadingDialogFragment.show(getFragmentManager());

        bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                         ignored -> LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> getFragmentManager().popBackStackImmediate()),
                         this::presentError);
    }

    public void pairNewPill(@NonNull View sender) {
        if (hardwarePresenter.getPeripheral() == null)
            return;

        Intent onboarding = new Intent(getActivity(), OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
        onboarding.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
        startActivity(onboarding);
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
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(hardwarePresenter.factoryReset(), device -> {
                Logger.info(getClass().getSimpleName(), "Completed Sense factory reset");
                Action0 finish = () -> {
                    LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                        apiSessionManager.logOut(getActivity());
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
        dialog.show();
    }

    public void unpairPill(@NonNull View sender) {
        SenseAlertDialog alertDialog = new SenseAlertDialog(getActivity());
        alertDialog.setDestructive(true);
        alertDialog.setTitle(R.string.dialog_title_unpair_pill);
        alertDialog.setMessage(R.string.dialog_message_unpair_pill);
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.action_unpair, (d, which) -> {
            activityIndicator.setVisibility(View.VISIBLE);
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                    ignored -> finishWithResult(RESULT_UNPAIRED_PILL, null),
                    this::presentError);
        });
        alertDialog.show();
    }
}
