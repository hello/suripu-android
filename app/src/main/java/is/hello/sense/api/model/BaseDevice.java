package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public abstract class BaseDevice extends ApiResponse {
    public static final int MISSING_THRESHOLD_HRS = 24;

    @SerializedName("state")
    public final State state;

    @SerializedName("id")
    public final String deviceId;

    @SerializedName("firmware_version")
    public final String firmwareVersion;

    @SerializedName("last_updated")
    public final DateTime lastUpdated;


    public BaseDevice(State state,
                      String deviceId,
                      String firmwareVersion,
                      DateTime lastUpdated) {
        this.state = state;
        this.deviceId = deviceId;
        this.firmwareVersion = firmwareVersion;
        this.lastUpdated = lastUpdated;
    }


    public @NonNull CharSequence getLastUpdatedDescription(@NonNull Context context) {
        if (lastUpdated != null) {
            return DateUtils.getRelativeTimeSpanString(context, lastUpdated);
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
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
        return ((lastUpdated == null) ||
                (getHoursSinceLastUpdated() >= MISSING_THRESHOLD_HRS));
    }

    @Override
    public String toString() {
        return "BaseDevice{" +
                "state=" + state +
                ", id='" + deviceId + '\'' +
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
}
