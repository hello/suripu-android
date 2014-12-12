package is.hello.sense.ui.common;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import is.hello.sense.R;

public final class Styles {
    public static final String TYPEFACE_BLACK = "fonts/AvenirLTCom-Black.ttf";
    public static final String TYPEFACE_BLACK_OBLIQUE = "fonts/AvenirLTCom-BlackOblique.ttf";
    public static final String TYPEFACE_HEAVY = "fonts/AvenirLTCom-Heavy.ttf";
    public static final String TYPEFACE_HEAVY_OBLIQUE = "fonts/AvenirLTCom-HeavyOblique.ttf";
    public static final String TYPEFACE_ULTRA_LIGHT = "fonts/AvenirNextLTPro-UltLt.ttf";
    public static final String TYPEFACE_LIGHT = "fonts/AvenirLTCom-Light.ttf";
    public static final String TYPEFACE_LIGHT_OBLIQUE = "fonts/AvenirLTCom-LightOblique.ttf";
    public static final String TYPEFACE_ROMAN = "fonts/AvenirLTCom-Roman.ttf";
    public static final String TYPEFACE_OBLIQUE = "fonts/AvenirLTCom-Oblique.ttf";

    public static @ColorRes @DrawableRes int getSleepDepthColorRes(int sleepDepth) {
        if (sleepDepth == 0)
            return R.color.sleep_awake;
        else if (sleepDepth == 100)
            return R.color.sleep_deep;
        else if (sleepDepth < 60)
            return R.color.sleep_light;
        else
            return R.color.sleep_intermediate;
    }

    public static @ColorRes @DrawableRes int getSleepDepthDimmedColorRes(int sleepDepth) {
        if (sleepDepth == 0)
            return R.color.sleep_awake_dimmed;
        else if (sleepDepth == 100)
            return R.color.sleep_deep_dimmed;
        else if (sleepDepth < 60)
            return R.color.sleep_light_dimmed;
        else
            return R.color.sleep_intermediate_dimmed;
    }

    public static @ColorRes @DrawableRes int getSleepScoreColorRes(int sleepScore) {
        if (sleepScore < 45)
            return R.color.sensor_warning;
        else if (sleepScore < 80)
            return R.color.sensor_alert;
        else
            return R.color.sensor_ideal;
    }
}
