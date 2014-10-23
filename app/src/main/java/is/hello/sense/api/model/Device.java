package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

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
        PILL,
        SENSE,
        OTHER;

        @JsonCreator
        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string, values(), OTHER);
        }
    }

    public static enum State {
        NORMAL,
        LOW_BATTERY,
        UNKNOWN;

        @JsonCreator
        public static State fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
