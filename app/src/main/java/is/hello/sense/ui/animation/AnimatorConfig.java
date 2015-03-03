package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.Interpolator;

import rx.functions.Action1;
import rx.functions.Func1;

public final class AnimatorConfig {
    public long duration = Animation.DURATION_NORMAL;
    public Interpolator interpolator = Animation.INTERPOLATOR_DEFAULT;
    public long startDelay = 0;

    public static final AnimatorConfig DEFAULT = AnimatorConfig.create();

    public static @NonNull AnimatorConfig create() {
        return new AnimatorConfig();
    }

    public static @NonNull AnimatorConfig create(@NonNull Action1<AnimatorConfig> visitor) {
        AnimatorConfig properties = create();
        visitor.call(properties);
        return properties;
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
