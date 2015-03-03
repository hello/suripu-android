package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.view.animation.Interpolator;

public final class AnimatorConfig {
    public long duration = Animation.DURATION_NORMAL;
    public Interpolator interpolator = Animation.INTERPOLATOR_DEFAULT;
    public long startDelay = 0;

    public static final AnimatorConfig DEFAULT = AnimatorConfig.create();

    public static @NonNull AnimatorConfig create() {
        return new AnimatorConfig();
    }

    public static @NonNull AnimatorConfig createWithDelay(long delay) {
        AnimatorConfig properties = create();
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
}
