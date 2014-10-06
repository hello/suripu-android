package is.hello.sense.util;

import android.animation.Animator;
import android.support.annotation.NonNull;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class Animation {
    public static final long DEFAULT_DURATION = 250;
    public static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    public static <T extends Animator> T applyDefaults(T animator) {
        animator.setDuration(DEFAULT_DURATION);
        animator.setInterpolator(DEFAULT_INTERPOLATOR);
        return animator;
    }


    public static final class Properties {
        public long duration = DEFAULT_DURATION;
        public Interpolator interpolator = DEFAULT_INTERPOLATOR;
        public long startDelay = 0;

        public static @NonNull Properties create() {
            return new Properties();
        }

        public static @NonNull Properties createWithDelay(long delay) {
            Properties properties = create();
            properties.startDelay = delay;
            return properties;
        }

        public <T extends Animator> T apply(@NonNull T animator) {
            animator.setDuration(duration);
            animator.setInterpolator(interpolator);
            animator.setStartDelay(startDelay);

            return animator;
        }

        public ViewPropertyAnimator apply(@NonNull ViewPropertyAnimator animator) {
            animator.setDuration(duration);
            animator.setInterpolator(interpolator);
            animator.setStartDelay(startDelay);

            return animator;
        }
    }
}
