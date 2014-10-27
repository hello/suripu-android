package is.hello.sense.api.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

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

    public @DrawableRes int getIconResource() {
        return getSensor().getResourceForCondition(getCondition());
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
        TEMPERATURE(R.drawable.temperature_good, R.drawable.temperature_medium, R.drawable.temperature_bad),
        HUMIDITY(R.drawable.humidity_good, R.drawable.humidity_medium, R.drawable.humidity_bad),
        PARTICULATES(R.drawable.particulates_good, R.drawable.particulates_medium, R.drawable.particulates_bad),
        SOUND(R.drawable.sound_good, R.drawable.sound_medium, R.drawable.sound_bad),
        LIGHT(R.drawable.light_good, R.drawable.light_medium, R.drawable.light_bad),
        UNKNOWN(R.drawable.movement_good, R.drawable.movement_medium, R.drawable.movement_bad);

        public final @DrawableRes int idealDrawableRes;
        public final @DrawableRes int warningDrawableRes;
        public final @DrawableRes int alertDrawableRes;

        public @DrawableRes int getResourceForCondition(Condition condition) {
            switch (condition) {
                case IDEAL:
                    return idealDrawableRes;

                case WARNING:
                    return warningDrawableRes;

                case ALERT:
                    return alertDrawableRes;

                default:
                case UNKNOWN:
                    return warningDrawableRes;
            }
        }

        private Sensor(@DrawableRes int idealDrawableRes,
                       @DrawableRes int warningDrawableRes,
                       @DrawableRes int alertDrawableRes) {
            this.idealDrawableRes = idealDrawableRes;
            this.warningDrawableRes = warningDrawableRes;
            this.alertDrawableRes = alertDrawableRes;
        }

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static Sensor fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }
}
