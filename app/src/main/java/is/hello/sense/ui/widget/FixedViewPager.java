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
public class FixedViewPager extends ViewPager {
    public FixedViewPager(@NonNull Context context) {
        super(context);
    }

    public FixedViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return super.onTouchEvent(event);
        } catch (IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }
}
