package is.hello.sense.api.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import is.hello.sense.R;

public class Device extends ApiResponse {
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

    @JsonIgnore
    private boolean exists = true;


    //region Creation

    public Device() {
    }

    public Device(@NonNull Type type, boolean exists) {
        this.type = type;
        this.exists = exists;
    }

    public static Device createPlaceholder(@NonNull Type type) {
        return new Device(type, false);
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

    @JsonIgnore
    public boolean exists() {
        return exists;
    }

    @Override
    public String toString() {
        return "Device{" +
                "type=" + type +
                ", deviceId='" + deviceId + '\'' +
                ", state=" + state +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", lastUpdated=" + lastUpdated +
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
        NORMAL(R.string.device_state_normal),
        LOW_BATTERY(R.string.device_state_low_battery),
        UNKNOWN(R.string.device_state_unknown);

        public final @StringRes int nameRes;

        private State(@StringRes int nameRes) {
            this.nameRes = nameRes;
        }

        @JsonCreator
        public static State fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
