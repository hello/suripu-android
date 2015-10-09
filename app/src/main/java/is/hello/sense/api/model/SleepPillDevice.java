package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

public class SleepPillDevice extends BaseDevice {
    @SerializedName("battery_level")
    public final long batteryLevel;


    public SleepPillDevice(@NonNull State state,
                           @NonNull Color color,
                           @NonNull String id,
                           @NonNull String firmwareVersion,
                           @NonNull DateTime lastUpdated,
                           long batteryLevel) {
        super(state, color, id, firmwareVersion, lastUpdated);
        this.batteryLevel = batteryLevel;
    }


    @Override
    public String toString() {
        return "SleepPillDevice{" +
                "batteryLevel=" + batteryLevel +
                '}';
    }
}
