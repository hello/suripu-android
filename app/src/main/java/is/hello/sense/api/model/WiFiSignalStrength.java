package is.hello.sense.api.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public enum WiFiSignalStrength implements Enums.FromString{

    NONE(R.string.accessibility_wifi_signal_strength_none, R.drawable.icon_wifi_none),
    BAD(R.string.accessibility_wifi_signal_strength_bad, R.drawable.icon_wifi_bad),
    FAIR(R.string.accessibility_wifi_signal_strength_fair, R.drawable.icon_wifi_fair),
    GOOD(R.string.accessibility_wifi_signal_strength_good, R.drawable.icon_wifi_good);

    public final @StringRes int accessibilityString;
    public final @DrawableRes int icon;

    WiFiSignalStrength(@StringRes int accessibilityString,
                       @DrawableRes int icon) {
        this.accessibilityString = accessibilityString;
        this.icon = icon;
    }

    public static WiFiSignalStrength fromRssi(int rssi) {
        if (rssi == 0) {
            return NONE;
        } else if (rssi <= -90) {
            return BAD;
        } else if (rssi <= -60) {
            return FAIR;
        } else {
            return GOOD;
        }
    }

    public static WiFiSignalStrength fromCondition(@NonNull String condition) {
        return Enums.fromString(condition, values(), NONE);
    }

}
