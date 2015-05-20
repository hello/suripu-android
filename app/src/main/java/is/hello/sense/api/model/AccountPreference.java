package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountPreference extends ApiResponse {
    @JsonProperty("pref")
    private Key key;

    @JsonProperty(value = "enabled", required = false)
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

    public AccountPreference setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }


    @Override
    public String toString() {
        return "AccountPreference{" +
                "key=" + key +
                ", enabled=" + enabled +
                '}';
    }

    public enum Key {
        ENHANCED_AUDIO,
        TEMP_CELSIUS,
        TIME_TWENTY_FOUR_HOUR,
        PUSH_ALERT_CONDITIONS,
        PUSH_SCORE,
        UNKNOWN;

        @JsonCreator
        public static Key fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
