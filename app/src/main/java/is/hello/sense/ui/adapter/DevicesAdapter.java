package is.hello.sense.ui.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.PlaceholderDevice;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class DevicesAdapter extends ArrayRecyclerAdapter<BaseDevice, DevicesAdapter.BaseViewHolder>
        implements View.OnClickListener {
    private static final int TYPE_PLACEHOLDER = 0;
    private static final int TYPE_SENSE = 1;
    private static final int TYPE_SLEEP_PILL = 2;

    private final LayoutInflater inflater;
    private final Resources resources;
    private final Activity activity;

    private
    @Nullable
    OnPairNewDeviceListener onPairNewDeviceListener;

    public DevicesAdapter(@NonNull Activity activity) {
        super(new ArrayList<>());
        this.activity = activity;
        this.inflater = LayoutInflater.from(activity);
        this.resources = activity.getResources();
    }


    public void setOnPairNewDeviceListener(@Nullable OnPairNewDeviceListener onPairNewDeviceListener) {
        this.onPairNewDeviceListener = onPairNewDeviceListener;
    }

    public void bindDevices(@NonNull Devices devices) {
        BaseDevice sense = devices.getSense();
        BaseDevice sleepPill = devices.getSleepPill();

        if (sense == null) {
            sense = new PlaceholderDevice(PlaceholderDevice.Type.SENSE);
        } else if (sleepPill == null) {
            sleepPill = new PlaceholderDevice(PlaceholderDevice.Type.SLEEP_PILL);
        }

        final List<BaseDevice> deviceList = new ArrayList<>(2);
        deviceList.add(sense);
        if (sleepPill != null) {
            deviceList.add(sleepPill);
        }
        replaceAll(deviceList);
    }

    public void devicesUnavailable(@SuppressWarnings("UnusedParameters") Throwable e) {
        clear();
    }

    @Override
    public int getItemViewType(int position) {
        final BaseDevice item = getItem(position);
        if (item instanceof PlaceholderDevice) {
            return TYPE_PLACEHOLDER;
        } else if (item instanceof SenseDevice) {
            return TYPE_SENSE;
        } else if (item instanceof SleepPillDevice) {
            return TYPE_SLEEP_PILL;
        } else {
            throw new IllegalArgumentException("Unknown type " + item.getClass());
        }
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_PLACEHOLDER: {
                final View view = inflater.inflate(R.layout.item_device_placeholder, parent, false);
                return new PlaceholderViewHolder(view);
            }
            case TYPE_SENSE: {
                final View view = inflater.inflate(R.layout.item_device, parent, false);
                return new SenseViewHolder(view);
            }
            case TYPE_SLEEP_PILL: {
                final View view = inflater.inflate(R.layout.item_device, parent, false);
                return new SleepPillViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onClick(View view) {
        if (onPairNewDeviceListener != null) {
            final PlaceholderDevice.Type type = (PlaceholderDevice.Type) view.getTag();
            onPairNewDeviceListener.onPairNewDevice(type);
        }
    }


    abstract class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        final TextView title;

        BaseViewHolder(@NonNull View view) {
            super(view);

            this.title = (TextView) view.findViewById(R.id.item_device_name);
        }

        abstract boolean wantsChevron();

        @Override
        public void bind(int position) {
            if (wantsChevron()) {
                @SuppressWarnings("ConstantConditions")
                final Drawable chevron = ResourcesCompat.getDrawable(resources,
                                                                     R.drawable.disclosure_chevron,
                                                                     null).mutate();
                Drawables.setTintColor(chevron, ContextCompat.getColor(activity, R.color.light_accent));
                title.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, chevron, null);
            } else {
                title.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            }
        }
    }

    class SenseViewHolder extends BaseViewHolder {
        final TextView lastSeen;
        final TextView status1;
        final TextView status1Label;
        final TextView status2;
        final TextView status2Label;

        SenseViewHolder(@NonNull View view) {
            super(view);

            this.lastSeen = (TextView) view.findViewById(R.id.item_device_last_seen);
            this.status1 = (TextView) view.findViewById(R.id.item_device_status);
            this.status1Label = (TextView) view.findViewById(R.id.item_device_status_label);
            this.status2 = (TextView) view.findViewById(R.id.item_device_status2);
            this.status2Label = (TextView) view.findViewById(R.id.item_device_status2_label);

            view.setOnClickListener(this);
        }

        @Override
        boolean wantsChevron() {
            return true;
        }

        @Override
        public void bind(int position) {
            super.bind(position);

            final SenseDevice device = (SenseDevice) getItem(position);
            title.setText(R.string.device_sense);
            lastSeen.setText(device.getLastUpdatedDescription(lastSeen.getContext()));
            if (device.isMissing()) {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.destructive_accent));
            } else {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
            }

            status1Label.setText(R.string.label_wifi);

            final SenseDevice.WiFiInfo wiFiInfo = device.wiFiInfo;
            if (wiFiInfo == null) {
                status1.setText(R.string.missing_data_placeholder);
            } else {
                final String networkName = wiFiInfo.ssid;
                if (TextUtils.isEmpty(networkName)) {
                    status1.setText(R.string.device_network_unknown);
                } else {
                    status1.setText(networkName);
                }
                if (wiFiInfo.getSignalStrength() != null) {
                    @DrawableRes int iconRes = wiFiInfo.getSignalStrength().icon;
                    status1.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
                } else {
                    status1.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                }
            }

            Styles.setTextAppearance(status1, R.style.AppTheme_Text_Body);

            status2Label.setText(R.string.label_firmware_version);
            status2.setText(device.firmwareVersion);
        }
    }

    class SleepPillViewHolder extends BaseViewHolder {
        final TextView lastSeen;
        final TextView status1;
        final TextView status1Label;
        final TextView status2;
        final TextView status2Label;
        final TextView status3;
        final TextView status3Label;
        final Button actionButton;

        SleepPillViewHolder(@NonNull View view) {
            super(view);

            this.lastSeen = (TextView) view.findViewById(R.id.item_device_last_seen);
            this.status1 = (TextView) view.findViewById(R.id.item_device_status);
            this.status1Label = (TextView) view.findViewById(R.id.item_device_status_label);
            this.status2 = (TextView) view.findViewById(R.id.item_device_status2);
            this.status2Label = (TextView) view.findViewById(R.id.item_device_status2_label);
            this.status3 = (TextView) view.findViewById(R.id.item_device_status3);
            this.status3Label = (TextView) view.findViewById(R.id.item_device_status3_label);
            this.actionButton = (Button) view.findViewById(R.id.item_device_action_button);

            this.status3Label.setVisibility(View.VISIBLE);
            this.status3.setVisibility(View.VISIBLE);

            Views.setTimeOffsetOnClickListener(view, this);
            Views.setTimeOffsetOnClickListener(actionButton, v -> {
                UserSupport.showUpdatePill(activity);
            });
            status2Label.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (event.getRawX() >= status2Label.getWidth() - status2Label.getCompoundDrawables()[2].getIntrinsicWidth()) {
                        WelcomeDialogFragment.show(activity, R.xml.welcome_dialog_pill_color, true);
                        return true;
                    }
                }
                return false;
            });
        }

        @Override
        boolean wantsChevron() {
            return true;
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final SleepPillDevice device = (SleepPillDevice) getItem(position);
            title.setText(R.string.device_pill);
            lastSeen.setText(device.getLastUpdatedDescription(lastSeen.getContext()));
            if (device.isMissing()) {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.destructive_accent));
            } else {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
            }

            status1Label.setText(R.string.label_battery_level);

            BaseDevice.State state = device.state;
            if (state == null) {
                state = BaseDevice.State.UNKNOWN;
            }
            status1.setText(state.nameRes);
            if (state == BaseDevice.State.UNKNOWN) {
                Styles.setTextAppearance(status1, R.style.AppTheme_Text_Body_Bold);
            } else {
                Styles.setTextAppearance(status1, R.style.AppTheme_Text_Body);
            }

            status2Label.setText(R.string.label_color);

            SleepPillDevice.Color color = device.color;
            if (color == null) {
                color = SleepPillDevice.Color.UNKNOWN;
            }
            status2.setText(color.nameRes);
            status2Label.setCompoundDrawablePadding(resources.getDimensionPixelSize(R.dimen.gap_medium));
            status2Label.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.info_button_icon_small, 0);

            status3Label.setText(R.string.label_firmware_version);
            status3.setText(device.firmwareVersion);

            if(device.shouldUpdate()){
                status3.setTextColor(ContextCompat.getColor(activity, R.color.warning));
                actionButton.setText(R.string.action_update);
                actionButton.setVisibility(View.VISIBLE);
            }
        }
    }

    class PlaceholderViewHolder extends BaseViewHolder {
        final TextView message;
        final Button actionButton;

        PlaceholderViewHolder(@NonNull View view) {
            super(view);

            this.message = (TextView) view.findViewById(R.id.item_device_placeholder_message);
            this.actionButton = (Button) view.findViewById(R.id.item_device_placeholder_action);
        }

        @Override
        boolean wantsChevron() {
            return false;
        }

        @Override
        public void bind(int position) {
            super.bind(position);

            final PlaceholderDevice device = (PlaceholderDevice) getItem(position);
            switch (device.type) {
                case SENSE: {
                    title.setText(R.string.device_sense);
                    message.setText(R.string.info_no_sense_connected);
                    actionButton.setText(R.string.action_pair_new_sense);
                    actionButton.setEnabled(true);
                    break;
                }

                case SLEEP_PILL: {
                    title.setText(R.string.device_pill);
                    message.setText(R.string.info_no_sleep_pill_connected);
                    actionButton.setText(R.string.action_pair_new_pill);
                    final boolean hasSense = (getItemCount() > 0 &&
                            DevicesAdapter.this.getItemViewType(0) != TYPE_PLACEHOLDER);
                    actionButton.setEnabled(hasSense);
                    break;
                }
            }

            actionButton.setTag(device.type);
            Views.setSafeOnClickListener(actionButton, DevicesAdapter.this);
        }
    }


    public interface OnPairNewDeviceListener {
        void onPairNewDevice(@NonNull PlaceholderDevice.Type type);
    }
}
