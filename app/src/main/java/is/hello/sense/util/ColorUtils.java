package is.hello.sense.util;

import android.support.annotation.ColorRes;

import is.hello.sense.R;

public final class ColorUtils {
    public static @ColorRes int colorResForSleepDepth(int sleepDepth) {
        if (sleepDepth == 0)
            return R.color.sleep_awake;
        else if (sleepDepth == 100)
            return R.color.sleep_deep;
        else if (sleepDepth < 60)
            return R.color.sleep_light;
        else
            return R.color.sleep_intermediate;
    }
}
