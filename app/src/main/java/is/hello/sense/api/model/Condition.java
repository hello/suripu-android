package is.hello.sense.api.model;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public enum Condition implements Enums.FromString {
    UNKNOWN(R.color.sensor_unknown),
    ALERT(R.color.sensor_alert),
    WARNING(R.color.sensor_warning),
    IDEAL(R.color.sensor_ideal);

    public final @ColorRes @DrawableRes int colorRes;

    Condition(@ColorRes int colorRes) {
        this.colorRes = colorRes;
    }

    public static Condition fromString(@NonNull String value) {
        return Enums.fromString(value, values(), UNKNOWN);
    }
}
