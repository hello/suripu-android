package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hello.ble.devices.Morpheus;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class DeviceDetailsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject DateFormatter dateFormatter;
    @Inject HardwarePresenter hardwarePresenter;

    private StaticItemAdapter.Item signalStrengthItem;
    private StaticItemAdapter adapter;

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

        Device device = (Device) getArguments().getSerializable(ARG_DEVICE);
        addPresenter(hardwarePresenter);

        this.adapter = new StaticItemAdapter(getActivity());
        adapter.addItem(getString(R.string.title_device_last_seen), dateFormatter.formatAsDate(device.getLastUpdated()));
        if (device.getType() == Device.Type.SENSE) {
            this.signalStrengthItem = adapter.addItem(getString(R.string.title_device_signal_strength), getString(R.string.missing_data_placeholder));
        }
        adapter.addItem(getString(R.string.title_device_firmware_version), device.getFirmwareVersion());
        adapter.addItem(getString(R.string.action_enter_pairing_mode), null, this::putIntoPairingMode);
        adapter.addItem(getString(R.string.action_factory_reset), null, this::factoryReset);

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

        bindAndSubscribe(this.hardwarePresenter.rediscoverDevice(), this::bindHardwareDevice, this::hardwareDeviceUnavailable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.hardwarePresenter.clearDevice();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        StaticItemAdapter.Item item = (StaticItemAdapter.Item) adapterView.getItemAtPosition(position);
        if (item.getAction() != null)
            item.getAction().run();
    }


    public void bindHardwareDevice(@Nullable Morpheus device) {
        if (device != null) {
            int rssi = device.getScanTimeRssi();
            String strength;
            if (rssi <= -30) {
                strength = getString(R.string.signal_strong);
            } else if (rssi <= -50) {
                strength = getString(R.string.signal_good);
            } else {
                strength = getString(R.string.signal_weak);
            }

            signalStrengthItem.setValue(strength);
        } else {
            Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.");
            signalStrengthItem.setValue(getString(R.string.missing_data_placeholder));
        }
    }

    public void hardwareDeviceUnavailable(Throwable e) {
        Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);
        signalStrengthItem.setValue(getString(R.string.missing_data_placeholder));
    }


    public void putIntoPairingMode() {
        LoadingDialogFragment.show(getFragmentManager());
    }

    public void factoryReset() {
    }
}
