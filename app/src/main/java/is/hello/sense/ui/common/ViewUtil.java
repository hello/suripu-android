package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.view.MotionEvent;

public final class ViewUtil {
    /**
     * Returns a given motion events X-coordinate, constrained to 0f or greater.
     */
    public static float getNormalizedX(@NonNull MotionEvent event) {
        return Math.max(0f, event.getX());
    }

    /**
     * Returns a given motion events Y-coordinate, constrained to 0f or greater.
     */
    public static float getNormalizedY(@NonNull MotionEvent event) {
        return Math.max(0f, event.getY());
    }
}
