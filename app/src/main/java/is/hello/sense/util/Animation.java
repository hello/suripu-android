package is.hello.sense.util;

import android.animation.Animator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class Animation {
    public static final long DURATION_MINIMUM = 150;
    public static final long DURATION_MAXIMUM = 350;
    public static final long DURATION_DEFAULT = 250;
    public static final Interpolator INTERPOLATOR_DEFAULT = new AccelerateDecelerateInterpolator();

    public static final class Properties {
        public long duration = DURATION_DEFAULT;
        public Interpolator interpolator = INTERPOLATOR_DEFAULT;
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

        public PropertyAnimatorProxy apply(@NonNull PropertyAnimatorProxy animator) {
            return animator.setDuration(duration)
                           .setInterpolator(interpolator)
                           .setStartDelay(startDelay);
        }


        public @NonNull PropertyAnimatorProxy toPropertyAnimator(@NonNull View forView) {
            return apply(new PropertyAnimatorProxy(forView));
        }
    }
}
