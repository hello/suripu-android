package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import rx.functions.Func1;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class Animations {
    public static final int DURATION_MINIMUM = 150;
    public static final int DURATION_MAXIMUM = 350;
    public static final int DURATION_DEFAULT = 250;
    public static final Interpolator INTERPOLATOR_DEFAULT = new DecelerateInterpolator();

    public static long calculateDuration(float velocity, float totalArea) {
        long rawDuration = (long) (totalArea / velocity) * 1000 / 2;
        return Math.max(Animations.DURATION_MINIMUM, Math.min(Animations.DURATION_MAXIMUM, rawDuration));
    }

    public static float interpolateFrame(float frameValue, float min, float max) {
        return min + frameValue * (max - min);
    }

    public static final class Properties {
        public long duration = DURATION_DEFAULT;
        public Interpolator interpolator = INTERPOLATOR_DEFAULT;
        public long startDelay = 0;

        public static final Properties DEFAULT = Properties.create();

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

        public LayoutTransition apply(@NonNull LayoutTransition transition) {
            transition.setDuration(duration);
            for (int transitionType = LayoutTransition.CHANGE_APPEARING;
                 transitionType <= LayoutTransition.CHANGING;
                 transitionType++) {
                transition.setStartDelay(transitionType, startDelay);
            }
            return transition;
        }


        public @NonNull PropertyAnimatorProxy toPropertyAnimator(@NonNull View forView) {
            return apply(new PropertyAnimatorProxy(forView));
        }
    }

    /**
     * A complex view group transition that should add a given new view,
     * and potentially remove some old child, with a fancy animation.
     *
     * @param <V>   The view group type.
     * @param <L>   The view group's layout params type.
     */
    public interface Transition<V extends ViewGroup, L extends ViewGroup.LayoutParams> {
        void perform(@NonNull V container,
                     @Nullable View newView,
                     @Nullable L layoutParams);
    }

    /**
     * Performs a simple cross fade from a given view group's
     * first child to an optional given new child.
     *
     * @see is.hello.sense.ui.animation.Animations.Transition
     */
    public static void crossFade(@NonNull ViewGroup container,
                                 @Nullable View newView,
                                 @Nullable ViewGroup.LayoutParams layoutParams) {
        if (container.getChildCount() > 0) {
            View oldView = container.getChildAt(0);
            animate(oldView)
                    .setDuration(Animations.DURATION_MINIMUM)
                    .fadeOut(View.VISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            container.removeView(oldView);
                            oldView.setAlpha(1f);
                        }
                    })
                    .start();
        }

        if (newView != null) {
            container.addView(newView, layoutParams);
            newView.setAlpha(0f);
            animate(newView)
                    .setDuration(Animations.DURATION_MINIMUM)
                    .fadeIn()
                    .start();
        }
    }
}
