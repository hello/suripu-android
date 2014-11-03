package is.hello.sense.ui.fragments.settings;

import android.app.AlertDialog;
import android.content.Intent;
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
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class DeviceDetailsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject DateFormatter dateFormatter;
    @Inject HardwarePresenter hardwarePresenter;

    private @Nullable StaticItemAdapter.Item signalStrengthItem;
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
        adapter.addItem(getString(R.string.action_select_wifi_network), null, this::changeWifiNetwork);
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

        LoadingDialogFragment.show(getFragmentManager());
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


    public void bindHardwareDevice(@NonNull Morpheus device) {
        int rssi = device.getScanTimeRssi();
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

        if (device.isConnected()) {
            LoadingDialogFragment.close(getFragmentManager());
        } else {
            bindAndSubscribe(hardwarePresenter.connectToDevice(device),
                             ignored -> LoadingDialogFragment.close(getFragmentManager()),
                             this::presentError);
        }
    }

    public void hardwareDeviceUnavailable(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        Logger.error(DeviceDetailsFragment.class.getSimpleName(), "Could not reconnect to Sense.", e);

        if (signalStrengthItem != null) {
            signalStrengthItem.setValue(getString(R.string.missing_data_placeholder));
        }
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    public void changeWifiNetwork() {
        Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, true);
        startActivity(intent);
    }

    @SuppressWarnings("CodeBlock2Expr")
    public void putIntoPairingMode() {
        Analytics.event(Analytics.EVENT_DEVICE_ACTION, Analytics.createProperties(Analytics.PROP_DEVICE_ACTION, Analytics.PROP_DEVICE_ACTION_ENABLE_PAIRING_MODE));

        if (hardwarePresenter.getDevice() == null)
            return;

        LoadingDialogFragment.show(getFragmentManager());

        bindAndSubscribe(hardwarePresenter.connectToDevice(hardwarePresenter.getDevice()), ignored -> {
            bindAndSubscribe(hardwarePresenter.putIntoPairingMode(), ignored1 -> {
                LoadingDialogFragment.close(getFragmentManager());
            }, this::presentError);
        }, this::presentError);
    }

    @SuppressWarnings("CodeBlock2Expr")
    public void factoryReset() {
        Analytics.event(Analytics.EVENT_DEVICE_ACTION, Analytics.createProperties(Analytics.PROP_DEVICE_ACTION, Analytics.PROP_DEVICE_ACTION_FACTORY_RESTORE));

        if (hardwarePresenter.getDevice() == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_title_factory_reset);
        builder.setMessage(R.string.dialog_messsage_factory_reset);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(R.string.action_factory_reset, (d, which) -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(hardwarePresenter.connectToDevice(hardwarePresenter.getDevice()), ignored -> {
                bindAndSubscribe(hardwarePresenter.factoryReset(), ignored1 -> {
                    LoadingDialogFragment.close(getFragmentManager());
                }, this::presentError);
            }, this::presentError);
        });
        builder.create().show();
    }
}
