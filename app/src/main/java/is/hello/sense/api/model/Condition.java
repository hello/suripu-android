package is.hello.sense.api.model;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public enum Condition implements Enums.FromString {
    UNKNOWN(R.color.dim,0),
    ALERT(R.color.sensor_alert,1),
    WARNING(R.color.sensor_warning,2),
    IDEAL(R.color.sensor_ideal,3);

    public final @ColorRes @DrawableRes int colorRes;
    public final int value;

    Condition(@ColorRes final int colorRes,
              final int value) {
        this.colorRes = colorRes;
        this.value = value;
    }

    public static Condition fromString(@NonNull final String value) {
        return Enums.fromString(value, values(), UNKNOWN);
    }
}
