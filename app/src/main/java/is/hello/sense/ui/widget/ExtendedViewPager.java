package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import is.hello.sense.util.Logger;

/**
 * Works around the open following open issues:
 * <ol>
 *     <li>https://code.google.com/p/android/issues/detail?id=18990</li>
 *     <li>https://code.google.com/p/android/issues/detail?id=66620</li>
 * </ol>
 */
public class ExtendedViewPager extends ViewPager {
    private boolean scrollingEnabled = true;

    public ExtendedViewPager(@NonNull Context context) {
        super(context);
    }

    public ExtendedViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return scrollingEnabled && super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return scrollingEnabled && super.onTouchEvent(event);
        } catch (IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return scrollingEnabled && super.canScrollHorizontally(direction);
    }

    public void setScrollingEnabled(boolean swipingEnabled) {
        this.scrollingEnabled = swipingEnabled;
    }
}
