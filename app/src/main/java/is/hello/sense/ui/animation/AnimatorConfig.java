package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.view.animation.Interpolator;

public final class AnimatorConfig {
    //region Attributes

    public final long duration;
    public final @NonNull Interpolator interpolator;

    //endregion


    //region Creation

    /**
     * The global default animator config. Uses {@link Animation#DURATION_NORMAL}
     * and {@link Animation#INTERPOLATOR_DEFAULT}.
     */
    public static final AnimatorConfig DEFAULT = new AnimatorConfig(Animation.DURATION_NORMAL, Animation.INTERPOLATOR_DEFAULT);

    public AnimatorConfig(long duration, @NonNull Interpolator interpolator) {
        this.duration = duration;
        this.interpolator = interpolator;
    }

    public AnimatorConfig(long duration) {
        this(duration, Animation.INTERPOLATOR_DEFAULT);
    }

    public AnimatorConfig(@NonNull Interpolator interpolator) {
        this(Animation.DURATION_NORMAL, interpolator);
    }

    //endregion


    //region Updating

    public AnimatorConfig withDuration(long newDuration) {
        return new AnimatorConfig(newDuration, interpolator);
    }

    //endregion


    //region Applying

    public <T extends Animator> T apply(@NonNull T animator) {
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);

        return animator;
    }

    public LayoutTransition apply(@NonNull LayoutTransition transition) {
        transition.setDuration(duration);
        return transition;
    }

    //endregion
}
