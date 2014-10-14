package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Insight extends ApiResponse {
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


    public static enum Sensor {
        TEMPERATURE,
        HUMIDITY,
        PARTICULATES,
        SOUND,
        LIGHT,
        UNKNOWN;

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static Sensor fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
