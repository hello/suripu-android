package is.hello.sense.api.model;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;

import is.hello.sense.R;

public enum Condition {
    UNKNOWN(R.color.sensor_unknown),
    IDEAL(R.color.sensor_ideal),
    WARNING(R.color.sensor_warning),
    ALERT(R.color.sensor_alert);

    public final @ColorRes @DrawableRes int colorRes;

    private Condition(@ColorRes int colorRes) {
        this.colorRes = colorRes;
    }

    @JsonCreator
    @SuppressWarnings("UnusedDeclaration")
    public static Condition fromString(@NonNull String value) {
        return Enums.fromString(value, values(), UNKNOWN);
    }
}
