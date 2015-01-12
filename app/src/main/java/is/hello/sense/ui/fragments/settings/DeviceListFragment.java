package is.hello.sense.ui.fragments.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.adapter.DevicesAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Styles;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class DeviceListFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final int DEVICE_REQUEST_CODE = 0x14;

    @Inject DevicesPresenter devicesPresenter;

    private ProgressBar loadingIndicator;
    private DevicesAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicesPresenter.update();
        addPresenter(devicesPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_device_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.adapter = new DevicesAdapter(getActivity());
        listView.setAdapter(adapter);

        Styles.addCardSpacingHeaderAndFooter(listView);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_settings_device_list_progress);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(devicesPresenter.devices, this::bindDevices, this::devicesUnavailable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DEVICE_REQUEST_CODE && resultCode == DeviceDetailsFragment.RESULT_UNPAIRED_PILL) {
            adapter.clear();

            animate(loadingIndicator)
                    .fadeIn()
                    .start();

            devicesPresenter.update();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Device device = (Device) adapterView.getItemAtPosition(position);
        DeviceDetailsFragment fragment = DeviceDetailsFragment.newInstance(device);
        fragment.setTargetFragment(this, DEVICE_REQUEST_CODE);
        ((FragmentNavigation) getActivity()).showFragment(fragment, getString(device.getType().nameRes), true);
    }


    public void bindDevices(@NonNull List<Device> devices) {
        animate(loadingIndicator)
                .fadeOut(View.GONE)
                .start();

        adapter.clear();
        adapter.addAll(devices);
    }

    public void devicesUnavailable(Throwable e) {
        animate(loadingIndicator)
                .fadeOut(View.GONE)
                .start();

        adapter.clear();

        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


}
