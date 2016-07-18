package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    @Nullable
    @SerializedName("firmware_update_url")
    public final String firmwareUpdateUrl;

    private boolean shouldUpdateOverride;


    public SleepPillDevice(final State state,
                           final Color color,
                           final String deviceId,
                           final String firmwareVersion,
                           final DateTime lastUpdated,
                           final long batteryLevel) {
        super(state, deviceId, firmwareVersion, lastUpdated);

        this.color = color;
        this.batteryLevel = batteryLevel;
        this.firmwareUpdateUrl = ""; //Todo update with real url if available
        this.shouldUpdateOverride = true;
    }

    public boolean shouldUpdate() {
        return firmwareUpdateUrl != null && !firmwareUpdateUrl.isEmpty();
    }
    public boolean shouldUpdateOverride(){
        return shouldUpdateOverride;
    }
    //todo remove when no longer need to suppress on client side
    public void setShouldUpdateOverride(final boolean override){
        this.shouldUpdateOverride = override;
    }

    public boolean hasLowBattery(){
        return batteryLevel <= 10; //Todo check with Jackson
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

        public static Color fromString(@NonNull final String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
