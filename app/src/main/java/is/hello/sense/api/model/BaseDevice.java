package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public abstract class BaseDevice extends ApiResponse {
    @SerializedName("state")
    public final State state;

    @SerializedName("color")
    public final Color color;

    @SerializedName("id")
    public final String id;

    @SerializedName("firmware_version")
    public final String firmwareVersion;

    @SerializedName("last_updated")
    public final DateTime lastUpdated;


    public BaseDevice(@NonNull State state,
                      @NonNull Color color,
                      @NonNull String id,
                      @NonNull String firmwareVersion,
                      @NonNull DateTime lastUpdated) {
        this.state = state;
        this.color = color;
        this.id = id;
        this.firmwareVersion = firmwareVersion;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "BaseDevice{" +
                "state=" + state +
                ", id='" + id + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }


    public enum State implements Enums.FromString {
        NORMAL(R.string.device_state_normal),
        LOW_BATTERY(R.string.device_state_low_battery),
        UNKNOWN(R.string.device_state_unknown);

        public final @StringRes
        int nameRes;

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
}
