package is.hello.sense.ui.animation;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * The default interpolator used by the <code>animation</code> package.
     */
    public static final Interpolator INTERPOLATOR_DEFAULT = new DecelerateInterpolator();

    /**
     * The views that are currently animating.
     */
    private static final Set<View> animatingViews = new HashSet<>();


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
        return animatingViews.contains(view);
    }

    /**
     * Adds a view to the currently animating set.
     */
    public static void addAnimatingView(@NonNull View view) {
        animatingViews.add(view);
    }

    /**
     * Removes a view from the currently animating set.
     */
    public static void removeAnimatingView(@NonNull View view) {
        animatingViews.remove(view);
    }

    //endregion


    @IntDef({View.VISIBLE, View.INVISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    @interface ViewVisibility {}

}
