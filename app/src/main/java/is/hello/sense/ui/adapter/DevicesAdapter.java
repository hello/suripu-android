package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.ui.widget.util.Views;

public class DevicesAdapter extends ArrayAdapter<Device> implements View.OnClickListener {
    private static final int ID_EXISTS = 0;
    private static final int ID_PLACEHOLDER = 1;

    private final LayoutInflater inflater;
    private final Resources resources;

    private @Nullable
    OnPairNewDeviceListener onPairNewDeviceListener;

    public DevicesAdapter(@NonNull Context context) {
        super(context, R.layout.item_device);

        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }


    public void setOnPairNewDeviceListener(@Nullable OnPairNewDeviceListener onPairNewDeviceListener) {
        this.onPairNewDeviceListener = onPairNewDeviceListener;
    }

    public void bindDevices(@NonNull List<Device> devices) {
        clear();

        Device sense = null;
        Device sleepPill = null;

        for (Device device : devices) {
            if (sense == null && device.getType() == Device.Type.SENSE) {
                sense = device;
            } else if (sleepPill == null && device.getType() == Device.Type.PILL) {
                sleepPill = device;
            }
        }

        if (sense == null) {
            sense = Device.createPlaceholder(Device.Type.SENSE);
        }

        if (sleepPill == null) {
            sleepPill = Device.createPlaceholder(Device.Type.PILL);
        }

        addAll(sense, sleepPill);
    }

    public void devicesUnavailable(@SuppressWarnings("UnusedParameters") Throwable e) {
        clear();
    }


    @Override
    public long getItemId(int position) {
        Device device = getItem(position);
        if (device.exists()) {
            return ID_EXISTS;
        } else {
            return ID_PLACEHOLDER;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            if (getItemId(position) == ID_EXISTS) {
                view = inflater.inflate(R.layout.item_device, parent, false);
                view.setTag(new DeviceViewHolder(view));
            } else {
                view = inflater.inflate(R.layout.item_device_placeholder, parent, false);
                view.setTag(new PlaceholderViewHolder(view));
            }
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        Device device = getItem(position);
        holder.display(device);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (onPairNewDeviceListener != null) {
            Device.Type type = (Device.Type) view.getTag();
            onPairNewDeviceListener.onPairNewDevice(type);
        }
    }


    class ViewHolder {
        final View itemView;
        final TextView title;

        ViewHolder(@NonNull View view) {
            this.itemView = view;

            this.title = (TextView) view.findViewById(R.id.item_device_name);
        }

        void display(@NonNull Device device) {
            title.setText(device.getType().nameRes);
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(device.getType().iconRes, 0, 0, 0);
        }
    }

    class DeviceViewHolder extends ViewHolder {
        final TextView lastSeen;
        final TextView status1;
        final TextView status1Label;
        final TextView status2;
        final TextView status2Label;

        DeviceViewHolder(@NonNull View view) {
            super(view);

            this.lastSeen = (TextView) view.findViewById(R.id.item_device_last_seen);
            this.status1 = (TextView) view.findViewById(R.id.item_device_status);
            this.status1Label = (TextView) view.findViewById(R.id.item_device_status_label);
            this.status2 = (TextView) view.findViewById(R.id.item_device_status2);
            this.status2Label = (TextView) view.findViewById(R.id.item_device_status2_label);
        }

        @Override
        void display(@NonNull Device device) {
            super.display(device);

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

    class PlaceholderViewHolder extends ViewHolder {
        final TextView message;
        final Button actionButton;

        PlaceholderViewHolder(@NonNull View view) {
            super(view);

            this.message = (TextView) view.findViewById(R.id.item_device_placeholder_message);
            this.actionButton = (Button) view.findViewById(R.id.item_device_placeholder_action);
        }

        @Override
        void display(@NonNull Device device) {
            super.display(device);

            switch (device.getType()) {
                case SENSE: {
                    message.setText(R.string.info_no_sense_connected);
                    actionButton.setText(R.string.action_pair_new_sense);
                    break;
                }

                default:
                case OTHER:
                case PILL: {
                    message.setText(R.string.info_no_sleep_pill_connected);
                    actionButton.setText(R.string.action_pair_new_pill);
                    break;
                }
            }

            actionButton.setTag(device.getType());
            Views.setSafeOnClickListener(actionButton, DevicesAdapter.this);
        }
    }


    public interface OnPairNewDeviceListener {
        void onPairNewDevice(@NonNull Device.Type type);
    }
}
