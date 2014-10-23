package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.common.InjectionFragment;

public class DeviceDetailsFragment extends InjectionFragment {
    private static final String ARG_DEVICE = DeviceDetailsFragment.class.getName() + ".ARG_DEVICE";

    @Inject HardwarePresenter hardwarePresenter;

    private Device device;

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

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list_view, container, false);

        return view;
    }
}
