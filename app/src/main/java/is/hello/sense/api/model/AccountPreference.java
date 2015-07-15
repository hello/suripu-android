package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.gson.Enums;

public class AccountPreference extends ApiResponse {
    @SerializedName("pref")
    private Key key;

    @SerializedName("enabled")
    private boolean enabled;


    public AccountPreference() {

    }

    public AccountPreference(@NonNull Key key) {
        this.key = key;
    }


    public Key getKey() {
        return key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    @Override
    public String toString() {
        return "AccountPreference{" +
                "key=" + key +
                ", enabled=" + enabled +
                '}';
    }

    public enum Key implements Enums.FromString {
        ENHANCED_AUDIO,
        TEMP_CELSIUS,
        TIME_TWENTY_FOUR_HOUR,
        PUSH_ALERT_CONDITIONS,
        PUSH_SCORE,
        UNKNOWN;

        public static Key fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
