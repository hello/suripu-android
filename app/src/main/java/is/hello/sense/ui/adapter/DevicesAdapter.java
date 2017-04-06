package is.hello.sense.ui.adapter;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.PlaceholderDevice;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class DevicesAdapter extends ArrayRecyclerAdapter<BaseDevice, DevicesAdapter.BaseViewHolder>
        implements View.OnClickListener {
    private static final int TYPE_PLACEHOLDER = 0;
    private static final int TYPE_SENSE = 1;
    private static final int TYPE_SLEEP_PILL = 2;
    private static final int TYPE_SUPPORT_PAIR_SECOND_PILL = 3;

    private final LayoutInflater inflater;
    private final Resources resources;
    private final Activity activity;

    private
    @Nullable
    OnDeviceInteractionListener onDeviceInteractionListener;
    private boolean hasSense = false;
    @Nullable
    private PlaceholderDevice senseWithVoicePlaceholder;

    public DevicesAdapter(@NonNull final Activity activity) {
        super(new ArrayList<>());
        this.activity = activity;
        this.inflater = LayoutInflater.from(activity);
        this.resources = activity.getResources();
    }


    public void setOnDeviceInteractionListener(@Nullable final OnDeviceInteractionListener onDeviceInteractionListener) {
        this.onDeviceInteractionListener = onDeviceInteractionListener;
    }

    public void bindDevices(@NonNull final Devices devices) {
        final SenseDevice sense = devices.getSense();
        final SleepPillDevice sleepPill = devices.getSleepPill();
        final List<BaseDevice> deviceList = new ArrayList<>(3);
        this.hasSense = sense != null;

        if (sense == null) {
            deviceList.add(new PlaceholderDevice(PlaceholderDevice.Type.SENSE));
            if (sleepPill != null) {
                deviceList.add(sleepPill);
            }
        } else {
            deviceList.add(sense);
            if (sleepPill == null) {
                deviceList.add(new PlaceholderDevice(PlaceholderDevice.Type.SLEEP_PILL));
            } else {
                deviceList.add(sleepPill);
            }
            deviceList.add(new PlaceholderDevice(PlaceholderDevice.Type.SUPPORT_PAIR_SECOND_PILL));

            if (sense.shouldUpgrade()) {
                deviceList.add(getSenseWithVoicePlaceHolder());
            }
        }

        replaceAll(deviceList);
    }

    @VisibleForTesting
    protected PlaceholderDevice getSenseWithVoicePlaceHolder() {
        if (this.senseWithVoicePlaceholder == null) {
            this.senseWithVoicePlaceholder = new PlaceholderDevice(PlaceholderDevice.Type.SENSE_WITH_VOICE);
            senseWithVoicePlaceholder.toggleCollapsed();
        }
        return this.senseWithVoicePlaceholder;
    }

    public void devicesUnavailable(@SuppressWarnings("UnusedParameters") Throwable e) {
        clear();
    }

    @Override
    public int getItemViewType(final int position) {
        final BaseDevice item = getItem(position);
        if (item instanceof PlaceholderDevice) {
            if(PlaceholderDevice.Type.SUPPORT_PAIR_SECOND_PILL.equals(((PlaceholderDevice) item).type)) {
                return TYPE_SUPPORT_PAIR_SECOND_PILL;
            } else {
                return TYPE_PLACEHOLDER;
            }
        } else if (item instanceof SenseDevice) {
            return TYPE_SENSE;
        } else if (item instanceof SleepPillDevice) {
            return TYPE_SLEEP_PILL;
        } else {
            throw new IllegalArgumentException("Unknown type " + item.getClass());
        }
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
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
            case TYPE_SUPPORT_PAIR_SECOND_PILL: {
                final View view = inflater.inflate(R.layout.item_device_support_footer, parent, false);
                return new SupportViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException("Unknown type " + viewType);
            }
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    @Override
    public void onViewDetachedFromWindow(BaseViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.onDetach();
    }

    @Override
    public void onClick(final View view) {
        if (onDeviceInteractionListener != null) {
            final PlaceholderDevice.Type type = (PlaceholderDevice.Type) view.getTag();
            onDeviceInteractionListener.onPlaceholderInteraction(type);
        }
    }

    private void dispatchOnScrollBy(final int x, final int y) {
        if (onDeviceInteractionListener != null) {
            onDeviceInteractionListener.onScrollBy(x, y);
        }
    }


    abstract class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        final TextView title;
        BaseViewHolder(@NonNull final View view) {
            super(view);

            this.title = (TextView) view.findViewById(R.id.item_device_name);
        }

        abstract boolean wantsChevron();

        @DrawableRes
        int getChevronRes() {
            return R.drawable.icon_chevron_right_24;
        }

        @Nullable
        Drawable getChevronDrawable() {
            if(!wantsChevron()) {
                return null;
            }
            Drawable chevron = title.getCompoundDrawables()[2];
            if(chevron == null) {
                //noinspection ConstantConditions
                chevron = ResourcesCompat.getDrawable(resources,
                                                      getChevronRes(),
                                                      null).mutate();
            }
            return chevron;
        }

        @Override
        public void bind(final int position) {
            final Drawable chevron = getChevronDrawable();
            if(chevron != null) {
                Drawables.setTintColor(chevron, ContextCompat.getColor(activity, R.color.devices_adapter_chevron));
            }
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, chevron, null);
        }

        void onDetach() {
            //do nothing
        }
    }

    class SupportViewHolder extends BaseViewHolder {

        SupportViewHolder(@NonNull final View view) {
            super(view);
        }

        @Override
        boolean wantsChevron() {
            return false;
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            Styles.initializeSupportFooter(DevicesAdapter.this.activity, this.title);
        }
    }

    abstract class DeviceViewHolder extends BaseViewHolder {

        final Button actionButton;

        DeviceViewHolder(@NonNull final View view) {
            super(view);

            this.actionButton = (Button) view.findViewById(R.id.item_device_action_button);

            Views.setTimeOffsetOnClickListener(actionButton, v -> {
                final int position = getAdapterPosition();
                if(onDeviceInteractionListener != null && position != RecyclerView.NO_POSITION) {
                    onDeviceInteractionListener.onUpdateDevice(getItem(position));
                }
            });
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final BaseDevice baseDevice = getItem(position);
            title.setText(baseDevice.getDisplayTitleRes());
        }
    }

    class SenseViewHolder extends DeviceViewHolder {
        final TextView lastSeen;
        final TextView status1;
        final TextView status1Label;
        final TextView status2;
        final TextView status2Label;

        SenseViewHolder(@NonNull final View view) {
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
        public void bind(final int position) {
            super.bind(position);

            final SenseDevice device = (SenseDevice) getItem(position);
            lastSeen.setText(device.getLastUpdatedDescription(lastSeen.getContext()));
            if (device.isMissing()) {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.error_text));
            } else {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.secondary_text));
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
                    @DrawableRes final int iconRes = wiFiInfo.getSignalStrength().icon;
                    status1.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
                } else {
                    status1.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                }
            }

            status2Label.setText(R.string.label_firmware_version);
            status2.setText(device.firmwareVersion);

            actionButton.setVisibility(View.GONE);
            actionButton.setEnabled(false);

        }
    }

    class SleepPillViewHolder extends DeviceViewHolder {
        final TextView lastSeen;
        final TextView status1;
        final TextView status1Label;
        final TextView status2;
        final TextView status2Label;
        final TextView status3;
        final TextView status3Label;

        SleepPillViewHolder(@NonNull final View view) {
            super(view);

            this.lastSeen = (TextView) view.findViewById(R.id.item_device_last_seen);
            this.status1 = (TextView) view.findViewById(R.id.item_device_status);
            this.status1Label = (TextView) view.findViewById(R.id.item_device_status_label);
            this.status2 = (TextView) view.findViewById(R.id.item_device_status2);
            this.status2Label = (TextView) view.findViewById(R.id.item_device_status2_label);
            this.status3 = (TextView) view.findViewById(R.id.item_device_status3);
            this.status3Label = (TextView) view.findViewById(R.id.item_device_status3_label);

            this.status3Label.setVisibility(View.VISIBLE);
            this.status3.setVisibility(View.VISIBLE);

            Views.setTimeOffsetOnClickListener(view, this);

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

            lastSeen.setText(device.getLastUpdatedDescription(lastSeen.getContext()));
            if (device.isMissing()) {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.error_text));
            } else {
                lastSeen.setTextColor(ContextCompat.getColor(activity, R.color.secondary_text));
            }

            status1Label.setText(R.string.label_battery_level);

            BaseDevice.State state = device.state;
            if (state == null) {
                state = BaseDevice.State.UNKNOWN;
            }
            status1.setText(state.nameRes);

            status2Label.setText(R.string.label_color);

            SleepPillDevice.Color color = device.color;
            if (color == null) {
                color = SleepPillDevice.Color.UNKNOWN;
            }
            status2.setText(color.nameRes);
            status2Label.setCompoundDrawablePadding(resources.getDimensionPixelSize(R.dimen.x2));
            status2Label.setCompoundDrawablesWithIntrinsicBounds(null,
                                                                 null,
                                                                 Styles.tintDrawable(activity,
                                                                                     R.drawable.icon_info_24,
                                                                                     R.color.primary_icon),
                                                                 null);
            status2Label.setGravity(Gravity.CENTER_VERTICAL);

            status3Label.setText(R.string.label_firmware_version);
            status3.setText(device.firmwareVersion);

            if(device.shouldUpdateOverride()){
                status3.setTextColor(ContextCompat.getColor(activity, R.color.warning));
                actionButton.setText(R.string.action_update);
                actionButton.setEnabled(true);
                actionButton.setVisibility(View.VISIBLE);
            } else {
                status3.setTextColor(ContextCompat.getColor(activity, R.color.secondary_text));
                actionButton.setVisibility(View.GONE);
                actionButton.setEnabled(false);
            }
        }
    }

    class PlaceholderViewHolder extends BaseViewHolder {
        final TextView message;
        final Button actionButton;
        boolean wantsChevron = false;
        static final int COLLAPSED_LEVEL = 0;
        static final int EXPANDED_LEVEL = 10000;
        @Nullable ValueAnimator animator;

        PlaceholderViewHolder(@NonNull final View view) {
            super(view);

            this.message = (TextView) view.findViewById(R.id.item_device_placeholder_message);
            this.actionButton = (Button) view.findViewById(R.id.item_device_placeholder_action);
        }

        @Override
        boolean wantsChevron() {
            return wantsChevron;
        }

        @Override
        @DrawableRes
        int getChevronRes() {
            return R.drawable.animated_up_down_chevron;
        }

        @Override
        public void bind(final int position) {
            final PlaceholderDevice device = (PlaceholderDevice) getItem(position);
            final boolean isCollapsed = device.isCollapsed();
            setBodyVisible(isCollapsed, true);
            switch (device.type) {
                case SENSE: {
                    title.setText(R.string.device_sense);
                    message.setText(R.string.info_no_sense_connected);
                    actionButton.setText(R.string.action_pair_sense);
                    actionButton.setEnabled(true);
                    wantsChevron = false;
                    animator = null;
                    break;
                }

                case SENSE_WITH_VOICE: {
                    title.setText(R.string.device_hardware_version_sense_with_voice);
                    message.setText(R.string.info_set_up_sense_with_voice);
                    Styles.initializeSupportFooter(activity, message);
                    actionButton.setText(R.string.action_set_up_sense_with_voice);
                    actionButton.setEnabled(true);
                    wantsChevron = true;
                    animator = ValueAnimator.ofInt(COLLAPSED_LEVEL,
                                                   EXPANDED_LEVEL);
                    animator.setDuration(Anime.DURATION_NORMAL);
                    title.setOnClickListener( ignored -> updateChevron(animator,
                                                                       !device.isCollapsed(),
                                                                       () -> {
                                                                           device.toggleCollapsed();
                                                                           PlaceholderViewHolder.this.setBodyVisible(device.isCollapsed(), false);
                                                                       },
                                                                       () -> {
                                                                           PlaceholderViewHolder.this.setBodyVisible(device.isCollapsed(), true);
                                                                           DevicesAdapter.this.dispatchOnScrollBy(0, this.itemView.getHeight());
                                                                       },
                                                                       () -> PlaceholderViewHolder.this.updateChevron(true)));
                    break;
                }

                case SLEEP_PILL: {
                    title.setText(R.string.device_pill);
                    message.setText(R.string.info_no_sleep_pill_connected);
                    actionButton.setText(R.string.action_pair_new_pill);
                    actionButton.setEnabled(DevicesAdapter.this.hasSense);
                    wantsChevron = false;
                    animator = null;
                    break;
                }
            }

            actionButton.setTag(device.type);
            Views.setSafeOnClickListener(actionButton, DevicesAdapter.this);
            super.bind(position);
            updateChevron(isCollapsed);
        }

        @Override
        public void onDetach() {
            if(animator != null && animator.isRunning()) {
                animator.cancel();
                animator = null;
            }
        }

        private void setBodyVisible(final boolean collapsed,
                                    final boolean gone) {
            final int visibility;
            if (collapsed) {
                visibility = gone ? View.GONE : View.INVISIBLE;
            } else {
                visibility = View.VISIBLE;
            }
            message.setVisibility(visibility);
            actionButton.setVisibility(visibility);
        }

        void updateChevron(final boolean collapsed) {
            final Drawable chevron = super.getChevronDrawable();
            if (chevron != null) {
                chevron.setLevel(collapsed ? COLLAPSED_LEVEL : EXPANDED_LEVEL);
            }
        }

        void updateChevron(@NonNull final ValueAnimator animator,
                           final boolean reverse,
                           @NonNull final Runnable onStart,
                           @NonNull final Runnable onComplete,
                           @NonNull final Runnable onCancel) {
            final Drawable chevron = super.getChevronDrawable();
            if (chevron == null || animator.isRunning()) {
                return;
            }

            animator.addUpdateListener(animation1 -> {
                if(animation1.isRunning()) {
                    chevron.setLevel((int) animation1.getAnimatedValue());
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    onStart.run();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    onComplete.run();
                    animation.removeAllListeners();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onCancel.run();
                    animation.removeAllListeners();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

            if(reverse) {
                animator.reverse();
            } else {
                animator.start();
            }
        }
    }

    public interface OnDeviceInteractionListener {
        void onPlaceholderInteraction(@NonNull PlaceholderDevice.Type type);
        void onUpdateDevice(@NonNull BaseDevice device);
        void onScrollBy(int x, int y);
    }
}
