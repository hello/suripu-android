package is.hello.sense.api.model;

import android.content.Context;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

    public Drawable getIcon(@NonNull Context context) {
        int color = context.getResources().getColor(getCondition().colorRes);
        Drawable icon = context.getResources().getDrawable(getSensor().iconRes);
        icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return icon;
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
        TEMPERATURE(R.drawable.before_sleep_insight_temperature),
        HUMIDITY(R.drawable.before_sleep_insight_humidity),
        PARTICULATES(R.drawable.before_sleep_insight_particulates),
        SOUND(R.drawable.before_sleep_insight_sound),
        LIGHT(R.drawable.before_sleep_insight_light),
        UNKNOWN(R.drawable.before_sleep_insight_phone);

        public final @DrawableRes int iconRes;

        private Sensor(@DrawableRes int iconRes) {
            this.iconRes = iconRes;
        }

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static Sensor fromString(@NonNull String string) {
            return Enums.fromString(string.toUpperCase(), values(), UNKNOWN);
        }
    }
}
