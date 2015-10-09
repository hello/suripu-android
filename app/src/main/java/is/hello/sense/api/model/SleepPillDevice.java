package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public class SleepPillDevice extends BaseDevice {
    @SerializedName("color")
    public final Color color;

    @SerializedName("battery_level")
    public final long batteryLevel;


    public SleepPillDevice(State state,
                           Color color,
                           String deviceId,
                           String firmwareVersion,
                           DateTime lastUpdated,
                           long batteryLevel) {
        super(state, deviceId, firmwareVersion, lastUpdated);

        this.color = color;
        this.batteryLevel = batteryLevel;
    }


    @Override
    public String toString() {
        return "SleepPillDevice{" +
                "batteryLevel=" + batteryLevel +
                '}';
    }

    public enum Color implements Enums.FromString {
        BLUE(R.string.device_color_blue),
        RED(R.string.device_color_red),
        UNKNOWN(R.string.missing_data_placeholder);

        public final @StringRes
        int nameRes;

        Color(int nameRes) {
            this.nameRes = nameRes;
        }

        public static Color fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
