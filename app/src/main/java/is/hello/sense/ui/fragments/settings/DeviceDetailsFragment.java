package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
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
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class DeviceDetailsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject ApiSessionManager apiSessionManager;
    @Inject DateFormatter dateFormatter;
    @Inject HardwarePresenter hardwarePresenter;

    private @Nullable StaticItemAdapter.Item signalStrengthItem;
    private StaticItemAdapter adapter;
    private Device device;
    private BluetoothAdapter bluetoothAdapter;

    private LoadingDialogFragment loadingDialogFragment;

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


        this.adapter = new StaticItemAdapter(getActivity());
        adapter.addItem(getString(R.string.title_device_last_seen), dateFormatter.formatAsDate(device.getLastUpdated()));
        adapter.addItem(getString(R.string.title_device_firmware_version), device.getFirmwareVersion());

        if (device.getType() == Device.Type.SENSE) {
            this.signalStrengthItem = adapter.addItem(getString(R.string.title_device_signal_strength), getString(R.string.missing_data_placeholder));
            adapter.addItem(getString(R.string.action_select_wifi_network), null, this::changeWifiNetwork);
            adapter.addItem(getString(R.string.action_enter_pairing_mode), null, this::putIntoPairingMode);
            adapter.addItem(getString(R.string.action_factory_reset), null, this::factoryReset);
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list_view, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (device.getType() == Device.Type.SENSE && bluetoothAdapter.isEnabled()) {
            this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_scanning_for_sense), false);
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
            if (signalStrengthItem != null) {
                signalStrengthItem.setValue(getString(R.string.missing_data_placeholder));
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

        if (signalStrengthItem != null) {
            signalStrengthItem.setValue(strength);
        }

        if (peripheral.isConnected()) {
            LoadingDialogFragment.close(getFragmentManager());
        } else {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(peripheral),
                    status -> {
                        switch (status) {
                            case CONNECTING:
                                loadingDialogFragment.setTitle(getString(R.string.title_connecting));
                                break;

                            case BONDING:
                                loadingDialogFragment.setTitle(getString(R.string.title_pairing));
                                break;

                            case DISCOVERING_SERVICES:
                                loadingDialogFragment.setTitle(getString(R.string.title_discovering_services));
                                break;

                            case CONNECTED:
                                LoadingDialogFragment.close(getFragmentManager());
                                break;
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
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }

        Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);

        if (signalStrengthItem != null) {
            signalStrengthItem.setValue(getString(R.string.missing_data_placeholder));
        }
    }


    public void changeWifiNetwork() {
        if (hardwarePresenter.getPeripheral() == null)
            return;

        Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, true);
        startActivity(intent);
    }

    public void putIntoPairingMode() {
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
    public void factoryReset() {
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
