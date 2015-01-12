package is.hello.sense.ui.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.DevicesPresenter;
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


    private static class DevicesAdapter extends ArrayAdapter<Device> {
        private final LayoutInflater inflater;
        private final Resources resources;

        private DevicesAdapter(@NonNull Context context) {
            super(context, R.layout.item_device);

            this.inflater = LayoutInflater.from(context);
            this.resources = context.getResources();
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
            holder.display(device);

            return view;
        }


        class ViewHolder {
            final TextView title;
            final TextView lastSeen;
            final TextView status1;
            final TextView status1Label;
            final TextView status2;
            final TextView status2Label;

            ViewHolder(@NonNull View view) {
                this.title = (TextView) view.findViewById(R.id.item_device_name);
                this.lastSeen = (TextView) view.findViewById(R.id.item_device_last_seen);
                this.status1 = (TextView) view.findViewById(R.id.item_device_status);
                this.status1Label = (TextView) view.findViewById(R.id.item_device_status_label);
                this.status2 = (TextView) view.findViewById(R.id.item_device_status2);
                this.status2Label = (TextView) view.findViewById(R.id.item_device_status2_label);
            }

            void display(@NonNull Device device) {
                title.setText(device.getType().nameRes);
                title.setCompoundDrawablesRelativeWithIntrinsicBounds(device.getType().iconRes, 0, 0, 0);

                lastSeen.setText(DateUtils.getRelativeTimeSpanString(getContext(), device.getLastUpdated()));
                if (Minutes.minutesBetween(device.getLastUpdated(), DateTime.now()).getMinutes() > 15) {
                    lastSeen.setTextColor(resources.getColor(R.color.destructuve_accent));
                } else {
                    lastSeen.setTextColor(resources.getColor(R.color.text_dark));
                }

                switch (device.getType()) {
                    case SENSE: {
                        status1Label.setText(R.string.label_wifi);
                        status1.setText(R.string.missing_data_placeholder);

                        status2Label.setText(R.string.label_firmware_version);
                        status2.setText(device.getFirmwareVersion());

                        break;
                    }

                    default:
                    case OTHER:
                    case PILL: {
                        status1Label.setText(R.string.label_battery_level);
                        status1Label.setText(device.getState().nameRes);

                        status2Label.setText(R.string.label_color);

                        break;
                    }
                }
            }
        }
    }
}
