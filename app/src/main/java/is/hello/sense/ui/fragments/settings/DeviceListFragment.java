package is.hello.sense.ui.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.DateFormatter;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class DeviceListFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject DevicesPresenter devicesPresenter;
    @Inject DateFormatter dateFormatter;

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

        this.adapter = new DevicesAdapter(getActivity(), dateFormatter);
        listView.setAdapter(adapter);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_settings_devices_progress);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(devicesPresenter.devices, this::bindDevices, this::devicesUnavailable);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Device device = (Device) adapterView.getItemAtPosition(position);
        DeviceDetailsFragment fragment = DeviceDetailsFragment.newInstance(device);
        ((SettingsActivity) getActivity()).showFragment(fragment, getString(device.getType().nameRes), true);
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


    private static class DevicesAdapter extends ArrayAdapter<Device> {
        private final LayoutInflater inflater;
        private final DateFormatter dateFormatter;

        private DevicesAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
            super(context, R.layout.item_device);

            this.inflater = LayoutInflater.from(context);
            this.dateFormatter = dateFormatter;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_device, parent, false);
                view.setTag(new ViewHolder(view));
            }

            ViewHolder holder = (ViewHolder) view.getTag();

            Device device = getItem(position);
            holder.icon.setImageResource(device.getType().iconRes);
            holder.title.setText(device.getType().nameRes);
            if (device.getState() == Device.State.LOW_BATTERY) {
                holder.status.setText(device.getState().nameRes);
            } else {
                String formattedDate = dateFormatter.formatAsDate(device.getLastUpdated());
                holder.status.setText(getContext().getString(R.string.device_last_seen_fmt, formattedDate));
            }

            return view;
        }


        private class ViewHolder {
            private final ImageView icon;
            private final TextView title;
            private final TextView status;

            private ViewHolder(@NonNull View view) {
                this.icon = (ImageView) view.findViewById(R.id.item_device_icon);
                this.title = (TextView) view.findViewById(R.id.item_device_name);
                this.status = (TextView) view.findViewById(R.id.item_device_status);
            }
        }
    }
}
