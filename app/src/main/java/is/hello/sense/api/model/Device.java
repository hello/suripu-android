package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.HashMap;
import java.util.Map;

import is.hello.sense.R;

public class Device extends ApiResponse {
    public static final int MISSING_THRESHOLD_HRS = 24;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("state")
    private State state;

    @JsonProperty("firmware_version")
    private String firmwareVersion;

    @JsonProperty("last_updated")
    private DateTime lastUpdated;

    @JsonProperty("color")
    private Color color;

    @JsonIgnore
    private boolean exists = true;


    //region Util

    public static Map<Type, Device> getDevicesMap(@NonNull Iterable<Device> devices) {
        Map<Type, Device> map = new HashMap<>();
        for (Device device : devices) {
            map.put(device.getType(), device);
        }
        return map;
    }

    //endregion


    //region Creation

    public static Device createPlaceholder(@NonNull Type type) {
        Device device = new Device();
        device.type = type;
        device.exists = false;
        return device;
    }

    //endregion


    public Type getType() {
        return type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public State getState() {
        return state;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public DateTime getLastUpdated() {
        return lastUpdated;
    }

    public @NonNull CharSequence getLastUpdatedDescription(@NonNull Context context) {
        if (lastUpdated != null) {
            return DateUtils.getRelativeTimeSpanString(context, lastUpdated);
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public Color getColor() {
        return color;
    }

    @JsonIgnore
    public boolean exists() {
        return exists;
    }

    /**
     * Returns the number of hours since the device was last updated.
     * <p/>
     * Returns 0 if the device has not reported being update yet. This state
     * happens immediately after a device has been paired to an account.
     */
    @JsonIgnore
    public int getHoursSinceLastUpdated() {
        if (lastUpdated != null) {
            return Hours.hoursBetween(lastUpdated, DateTime.now()).getHours();
        } else {
            return 0;
        }
    }

    /**
     * Returns whether or not the device is considered to be missing.
     * <p/>
     * Differs from {@link #getHoursSinceLastUpdated()} by considering
     * a missing last updated value to indicate a device is missing.
     */
    @JsonIgnore
    public boolean isMissing() {
        return (!exists || (getLastUpdated() == null) ||
                (getHoursSinceLastUpdated() >= MISSING_THRESHOLD_HRS));
    }

    @Override
    public String toString() {
        return "Device{" +
                "type=" + type +
                ", deviceId='" + deviceId + '\'' +
                ", state=" + state +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", color=" + color +
                ", exists=" + exists +
                '}';
    }


    public static enum Type {
        PILL(R.drawable.pill_icon, R.string.device_pill),
        SENSE(R.drawable.sense_icon, R.string.device_sense),
        OTHER(R.drawable.sense_icon, R.string.device_unknown);


        public final @DrawableRes int iconRes;
        public final @StringRes int nameRes;

        private Type(@DrawableRes int iconRes, @StringRes int nameRes) {
            this.iconRes = iconRes;
            this.nameRes = nameRes;
        }


        @JsonCreator
        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string, values(), OTHER);
        }
    }

    public static enum State {
        NORMAL(R.string.device_state_normal, R.color.text_dark),
        LOW_BATTERY(R.string.device_state_low_battery, R.color.destructive_accent),
        UNKNOWN(R.string.device_state_unknown, R.color.destructive_accent);

        public final @StringRes int nameRes;
        public final @ColorRes int colorRes;

        private State(@StringRes int nameRes, @ColorRes int colorRes) {
            this.nameRes = nameRes;
            this.colorRes = colorRes;
        }

        @JsonCreator
        public static State fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }

    public static enum Color {
        BLACK(R.string.device_color_black),
        WHITE(R.string.device_color_white),
        BLUE(R.string.device_color_blue),
        RED(R.string.device_color_red),
        UNKNOWN(R.string.missing_data_placeholder);

        public final @StringRes int nameRes;

        private Color(int nameRes) {
            this.nameRes = nameRes;
        }

        @JsonCreator
        public static Color fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
