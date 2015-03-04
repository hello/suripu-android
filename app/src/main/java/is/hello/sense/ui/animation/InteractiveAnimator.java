package is.hello.sense.ui.animation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.Interpolator;

/**
 * An animation that starts as interactive, and completes as non-interactive.
 */
public interface InteractiveAnimator {
    /**
     * Perform any preparations necessary for the animator to do its work.
     */
    void prepare();

    /**
     * Move the state of the animated body to a given frame value.
     * @param frameValue Generally within {0, 1}, may go over or under in response
     *                   to an interaction overshooting its bounds (elastic banding).
     */
    void frame(float frameValue);

    /**
     * Complete the changes made to the animated body in a non-interactive manner.
     * @param finalFrameValue   Always within {0, 1}.
     * @param duration          The amount of time the non-interactive portion
     *                          of the animation is intended to take.
     * @param interpolator      The interpolator to use.
     * @param animatorContext   The context to run the animations within.
     */
    void finish(float finalFrameValue,
                long duration,
                @NonNull Interpolator interpolator,
                @Nullable AnimatorContext animatorContext);

    /**
     * Cancel any non-interactive animations currently in progress.
     * <p/>
     * This method may be called even when there are
     * no interactive animations currently happening.
     */
    void cancel();
}
