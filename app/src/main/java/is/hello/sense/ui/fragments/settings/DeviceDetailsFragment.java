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
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class DeviceDetailsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject ApiSessionManager apiSessionManager;
    @Inject DateFormatter dateFormatter;
    @Inject HardwarePresenter hardwarePresenter;

    private ProgressBar pairingActivityIndicator;
    private ViewGroup senseActionsContainer;
    private @Nullable SensorStateView signalStrength;

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

        if (device.getType() == Device.Type.SENSE) {
            this.pairingActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_device_details_sense_pairing);
            this.senseActionsContainer = (ViewGroup) view.findViewById(R.id.fragment_device_details_sense);
            this.signalStrength = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_signal);

            SensorStateView changeWifi = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_change_wifi);
            changeWifi.setOnClickListener(this::changeWifiNetwork);

            SensorStateView enterPairingMode = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_pairing_mode);
            enterPairingMode.setOnClickListener(this::putIntoPairingMode);

            SensorStateView factoryReset = (SensorStateView) view.findViewById(R.id.fragment_device_details_sense_factory_reset);
            factoryReset.setOnClickListener(this::factoryReset);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (device.getType() == Device.Type.SENSE && bluetoothAdapter.isEnabled()) {
            pairingActivityIndicator.setVisibility(View.VISIBLE);
            senseActionsContainer.setVisibility(View.GONE);

            bindAndSubscribe(this.hardwarePresenter.discoverPeripheralForDevice(device), this::bindPeripheral, this::presentError);
            bindAndSubscribe(this.hardwarePresenter.bluetoothEnabled, this::onBluetoothStateChanged, Functions.LOG_ERROR);
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
        if (!isEnabled) {
            if (signalStrength != null) {
                signalStrength.setReading(getString(R.string.missing_data_placeholder));
                senseActionsContainer.setVisibility(View.GONE);
                pairingActivityIndicator.setVisibility(View.GONE);
            }
        }
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
            pairingActivityIndicator.setVisibility(View.GONE);
        } else {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(peripheral),
                             status -> {
                                 if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                                     senseActionsContainer.setVisibility(View.VISIBLE);
                                     pairingActivityIndicator.setVisibility(View.GONE);
                                 }
                             },
                             this::presentError);
        }
    }

    public void presentError(@NonNull Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (hardwarePresenter.isErrorFatal(e)) {
            ErrorDialogFragment.presentFatalBluetoothError(getFragmentManager(), getActivity());
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }

        Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);

        if (signalStrength != null) {
            signalStrength.setReading(getString(R.string.missing_data_placeholder));
            senseActionsContainer.setVisibility(View.GONE);
            pairingActivityIndicator.setVisibility(View.GONE);
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
        Analytics.event(Analytics.EVENT_DEVICE_ACTION, Analytics.createProperties(Analytics.PROP_DEVICE_ACTION, Analytics.PROP_DEVICE_ACTION_ENABLE_PAIRING_MODE));

        if (hardwarePresenter.getPeripheral() == null)
            return;

        LoadingDialogFragment.show(getFragmentManager());

        bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             getFragmentManager().popBackStackImmediate();
                         },
                         this::presentError);
    }

    @SuppressWarnings("CodeBlock2Expr")
    public void factoryReset(@NonNull View sender) {
        Analytics.event(Analytics.EVENT_DEVICE_ACTION, Analytics.createProperties(Analytics.PROP_DEVICE_ACTION, Analytics.PROP_DEVICE_ACTION_FACTORY_RESTORE));

        if (hardwarePresenter.getPeripheral() == null)
            return;

        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setTitle(R.string.dialog_title_factory_reset);
        dialog.setMessage(R.string.dialog_messsage_factory_reset);
        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(hardwarePresenter.factoryReset(), ignored -> {
                LoadingDialogFragment.close(getFragmentManager());
                apiSessionManager.logOut(getActivity());
                getActivity().finish();
            }, this::presentError);
        });
        dialog.setDestructive(true);
        dialog.show();
    }
}
