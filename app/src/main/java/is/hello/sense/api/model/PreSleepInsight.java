package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import is.hello.sense.R;

public class PreSleepInsight extends ApiResponse {
    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("message")
    private String message;

    @JsonProperty("sensor")
    private Sensor sensor;


    public Condition getCondition() {
        return condition;
    }

    public String getMessage() {
        return message;
    }

    public Sensor getSensor() {
        return sensor;
    }


    @Override
    public String toString() {
        return "Insight{" +
                "condition=" + condition +
                ", message='" + message + '\'' +
                ", sensor=" + sensor +
                '}';
    }


    public enum Sensor {
        TEMPERATURE(R.string.condition_temperature),
        HUMIDITY(R.string.condition_humidity),
        PARTICULATES(R.string.condition_particulates),
        SOUND(R.string.condition_sound),
        LIGHT(R.string.condition_light),
        UNKNOWN(R.string.missing_data_placeholder);

        public final @StringRes int titleRes;

        Sensor(@StringRes int titleRes) {
            this.titleRes = titleRes;
        }

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static Sensor fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }
}
