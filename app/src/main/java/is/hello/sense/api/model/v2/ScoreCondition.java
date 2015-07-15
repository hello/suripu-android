package is.hello.sense.api.model.v2;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.model.Enums;

public enum ScoreCondition implements Enums.FromString {
    UNAVAILABLE(R.color.sensor_unknown),
    ALERT(R.color.sensor_alert),
    WARNING(R.color.sensor_warning),
    IDEAL(R.color.sensor_ideal);

    public final @ColorRes @DrawableRes int colorRes;

    ScoreCondition(@ColorRes int colorRes) {
        this.colorRes = colorRes;
    }

    public static ScoreCondition fromString(@NonNull String string) {
        return Enums.fromString(string, values(), UNAVAILABLE);
    }
}
