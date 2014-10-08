package is.hello.sense.util;

import android.support.annotation.ColorRes;

import is.hello.sense.R;

public final class Styles {
    public static @ColorRes int getSleepDepthColorRes(int sleepDepth) {
        if (sleepDepth == 0)
            return R.color.sleep_awake;
        else if (sleepDepth == 100)
            return R.color.sleep_deep;
        else if (sleepDepth < 60)
            return R.color.sleep_light;
        else
            return R.color.sleep_intermediate;
    }

    public static @ColorRes int getSleepScoreColorRes(int sleepScore) {
        if (sleepScore < 45)
            return R.color.sensor_warning;
        else if (sleepScore < 80)
            return R.color.sensor_alert;
        else
            return R.color.sensor_ideal;
    }
}
