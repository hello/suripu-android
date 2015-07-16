package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.HashMap;
import java.util.Map;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public class Device extends ApiResponse {
    public static final int MISSING_THRESHOLD_HRS = 24;

    @SerializedName("type")
    private Type type;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("state")
    private State state;

    @SerializedName("firmware_version")
    private String firmwareVersion;

    @SerializedName("last_updated")
    private DateTime lastUpdated;

    @SerializedName("color")
    private Color color;

    @Expose(deserialize = false, serialize = false)
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
        device.state = State.UNKNOWN;
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

    public boolean exists() {
        return exists;
    }

    /**
     * Returns the number of hours since the device was last updated.
     * <p/>
     * Returns 0 if the device has not reported being update yet. This state
     * happens immediately after a device has been paired to an account.
     */
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


    public enum Type implements Enums.FromString {
        PILL(R.drawable.pill_icon, R.string.device_pill),
        SENSE(R.drawable.sense_icon, R.string.device_sense),
        OTHER(R.drawable.sense_icon, R.string.device_unknown);


        public final @DrawableRes int iconRes;
        public final @StringRes int nameRes;

        Type(@DrawableRes int iconRes, @StringRes int nameRes) {
            this.iconRes = iconRes;
            this.nameRes = nameRes;
        }


        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string, values(), OTHER);
        }
    }

    public enum State implements Enums.FromString {
        NORMAL(R.string.device_state_normal),
        LOW_BATTERY(R.string.device_state_low_battery),
        UNKNOWN(R.string.device_state_unknown);

        public final @StringRes int nameRes;

        State(@StringRes int nameRes) {
            this.nameRes = nameRes;
        }

        public static State fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }

    public enum Color implements Enums.FromString {
        BLACK(R.string.device_color_black),
        WHITE(R.string.device_color_white),
        BLUE(R.string.device_color_blue),
        RED(R.string.device_color_red),
        UNKNOWN(R.string.missing_data_placeholder);

        public final @StringRes int nameRes;

        Color(int nameRes) {
            this.nameRes = nameRes;
        }

        public static Color fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }


    @VisibleForTesting
    public static class Builder {
        private final Device device = new Device();

        public Builder(@NonNull Type type) {
            device.type = type;
            setDeviceId("473CC31A-D5D7-4AAD-B9EE-31B6785E6A0E");
            setState(State.UNKNOWN);
            setFirmwareVersion("ffffff");
            setColor(Color.UNKNOWN);
        }

        public Builder setDeviceId(@NonNull String deviceId) {
            device.deviceId = deviceId;
            return this;
        }

        public Builder setState(@NonNull State state) {
            device.state = state;
            return this;
        }

        public Builder setFirmwareVersion(@NonNull String firmwareVersion) {
            device.firmwareVersion = firmwareVersion;
            return this;
        }

        public Builder setLastUpdated(@NonNull DateTime lastUpdated) {
            device.lastUpdated = lastUpdated;
            return this;
        }

        public Builder setColor(@NonNull Color color) {
            device.color = color;
            return this;
        }

        public Device build() {
            return device;
        }
    }
}
