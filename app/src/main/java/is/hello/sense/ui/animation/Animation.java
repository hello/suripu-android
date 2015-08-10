package is.hello.sense.ui.animation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

import is.hello.sense.ui.widget.RectEvaluatorCompat;

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

    /**
     * The views that are currently animating.
     */
    static final Set<View> ANIMATING_VIEWS = new HashSet<>();


    public static long calculateDuration(float velocity, float totalArea) {
        long rawDuration = (long) (totalArea / velocity) * 1000 / 2;
        return Math.max(Animation.DURATION_FAST, Math.min(Animation.DURATION_SLOW, rawDuration));
    }

    public static float interpolateFrame(float frameValue, float min, float max) {
        return min + frameValue * (max - min);
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


    //region Animating Views

    /**
     * Stops any running animation on a given array of views.
     */
    public static void cancelAll(@NonNull View... forViews) {
        for (View forView : forViews) {
            forView.animate().cancel();
            forView.clearAnimation();
        }
    }

    /**
     * Returns whether or not a given view is known to be animating.
     */
    public static boolean isAnimating(@NonNull View view) {
        return ANIMATING_VIEWS.contains(view);
    }

    //endregion


    @IntDef({View.VISIBLE, View.INVISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    @interface ViewVisibility {}

}
