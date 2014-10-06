package is.hello.sense.util;

import android.animation.Animator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class Animation {
    public static final int DEFAULT_DURATION = 250;
    public static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    public static <T extends Animator> T applyDefaults(T animator) {
        animator.setDuration(DEFAULT_DURATION);
        animator.setInterpolator(DEFAULT_INTERPOLATOR);
        return animator;
    }
}
