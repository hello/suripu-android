package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import rx.functions.Func1;

public class Animation {
    public static final long DURATION_MINIMUM = 150;
    public static final long DURATION_MAXIMUM = 350;
    public static final long DURATION_DEFAULT = 250;
    public static final Interpolator INTERPOLATOR_DEFAULT = new AccelerateDecelerateInterpolator();

    public static long durationFromVelocityTracker(@NonNull VelocityTracker velocityTracker, float totalArea) {
        velocityTracker.computeCurrentVelocity(1000);

        float velocity = Math.abs(velocityTracker.getXVelocity());
        long rawDuration = (long) (totalArea / velocity) * 1000 / 2;
        return Math.max(Animation.DURATION_MINIMUM, Math.min(Animation.DURATION_MAXIMUM, rawDuration));
    }

    public static final class Properties {
        public long duration = DURATION_DEFAULT;
        public Interpolator interpolator = INTERPOLATOR_DEFAULT;
        public long startDelay = 0;

        public static Properties DEFAULT = Properties.create();

        public static @NonNull Properties create() {
            return new Properties();
        }

        public static @NonNull Properties create(@NonNull Func1<Properties, Void> f) {
            Properties properties = create();
            f.call(properties);
            return properties;
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

        public LayoutTransition apply(@NonNull LayoutTransition transition, boolean applyInterpolator) {
            transition.setDuration(duration);
            for (int transitionType = LayoutTransition.CHANGE_APPEARING;
                 transitionType <= LayoutTransition.CHANGING;
                 transitionType++) {
                transition.setStartDelay(transitionType, startDelay);

                if (applyInterpolator)
                    transition.setInterpolator(transitionType, interpolator);
            }
            return transition;
        }


        public @NonNull PropertyAnimatorProxy toPropertyAnimator(@NonNull View forView) {
            return apply(new PropertyAnimatorProxy(forView));
        }
    }
}
