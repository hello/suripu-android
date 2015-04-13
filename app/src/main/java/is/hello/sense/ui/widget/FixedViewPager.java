package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import is.hello.sense.util.Logger;

/**
 * Works around the open issue https://code.google.com/p/android/issues/detail?id=18990.
 */
public class FixedViewPager extends ViewPager {
    public FixedViewPager(Context context) {
        super(context);
    }

    public FixedViewPager(Context context, AttributeSet attrs) {
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
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }
}
