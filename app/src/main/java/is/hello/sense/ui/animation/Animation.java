package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import is.hello.sense.ui.widget.RectEvaluatorCompat;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class Animation {
    /**
     * For animations that will be run in the middle of a user interaction
     * where just snapping an element off the screen would look bad.
     */
    public static final int DURATION_VERY_FAST = 50;

    /**
     * The fastest speed used for a regular animation.
     */
    public static final int DURATION_FAST = 150;

    /**
     * The slowest speed used for a regular animation.
     */
    public static final int DURATION_SLOW = 350;

    /**
     * Typical duration for animations in the Sense app. The
     * original duration constant used by iOS before version 7.
     */
    public static final int DURATION_NORMAL = 250;

    public static final Interpolator INTERPOLATOR_DEFAULT = new DecelerateInterpolator();

    public static long calculateDuration(float velocity, float totalArea) {
        long rawDuration = (long) (totalArea / velocity) * 1000 / 2;
        return Math.max(Animation.DURATION_FAST, Math.min(Animation.DURATION_SLOW, rawDuration));
    }

    public static float interpolateFrame(float frameValue, float min, float max) {
        return min + frameValue * (max - min);
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
                     @Nullable L layoutParams,
                     @Nullable Runnable onCompletion);
    }

    /**
     * Performs a simple cross fade from a given view group's
     * first child to an optional given new child.
     *
     * @see Animation.Transition
     */
    public static void crossFade(@NonNull ViewGroup container,
                                 @Nullable View newView,
                                 @Nullable ViewGroup.LayoutParams layoutParams,
                                 @Nullable Runnable onCompletion) {
        if (container.getChildCount() > 0) {
            View oldView = container.getChildAt(0);
            animate(oldView)
                    .setDuration(Animation.DURATION_FAST)
                    .fadeOut(View.VISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            container.removeView(oldView);
                            oldView.setAlpha(1f);

                            if (onCompletion != null) {
                                onCompletion.run();
                            }
                        }
                    })
                    .start();
        }

        if (newView != null) {
            container.addView(newView, layoutParams);
            newView.setAlpha(0f);
            animate(newView)
                    .setDuration(Animation.DURATION_FAST)
                    .fadeIn()
                    .start();
        }
    }

    /**
     * Creates and returns a ValueAnimator that will
     * transition between the specified array of colors.
     * <p/>
     * Returned animator is configured to use the standard defaults.
     */
    public static ValueAnimator createColorAnimator(@NonNull int... colors) {
        ValueAnimator colorAnimator = ValueAnimator.ofInt((int[]) colors);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setInterpolator(INTERPOLATOR_DEFAULT);
        colorAnimator.setDuration(DURATION_NORMAL);
        return colorAnimator;
    }

    /**
     * Creates and returns a ValueAnimator that will
     * transition between the specified array of rectangles.
     * <p/>
     * The same Rect instance will be used in each call.
     */
    public static ValueAnimator createRectAnimator(@NonNull Rect... rectangles) {
        ValueAnimator rectAnimator = ValueAnimator.ofObject(new RectEvaluatorCompat(), (Object[]) rectangles);
        rectAnimator.setInterpolator(INTERPOLATOR_DEFAULT);
        rectAnimator.setDuration(DURATION_NORMAL);
        return rectAnimator;
    }

    public static ValueAnimator createViewFrameAnimator(@NonNull View view, @NonNull Rect... rectangles) {
        ValueAnimator frameAnimator = createRectAnimator((Rect[]) rectangles);
        frameAnimator.addUpdateListener(a -> {
            Rect frame = (Rect) a.getAnimatedValue();
            view.layout(frame.left, frame.top, frame.right, frame.bottom);
        });
        return frameAnimator;
    }

    /**
     * Creates and returns an animator that does nothing.
     */
    public static Animator createEmptyAnimator() {
        return new AnimatorSet();
    }
}
