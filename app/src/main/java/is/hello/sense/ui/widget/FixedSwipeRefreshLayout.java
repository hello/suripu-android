package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

import is.hello.sense.util.Logger;

/**
 * SwipeRefreshLayout is missing a redundant check for invalid pointers in its
 * onTouchEvent's MotionEvent.ACTION_CANCEL handling clause. If the user has two
 * fingers down, then does an action which causes the swipe refresh layout to be
 * removed from its parent view, the cancel event will not have the original
 * pointer the swipe refresh layout was tracking causing an out of bounds exception.
 * <p/>
 * Solution derived from
 * http://stackoverflow.com/questions/27662682/illegalargumentexception-pointerindex-out-of-range-from-swiperefreshlayout
 * <p/>
 * Bug thread here
 * https://code.google.com/p/android/issues/detail?id=163954
 */
public class FixedSwipeRefreshLayout extends SwipeRefreshLayout {
    private int activePointerId = -1;

    public FixedSwipeRefreshLayout(Context context) {
        super(context);
    }

    public FixedSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                this.activePointerId = MotionEventCompat.getPointerId(event, 0);
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                int pointerIndex = MotionEventCompat.getActionIndex(event);
                this.activePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = MotionEventCompat.getActionIndex(event);
                int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.activePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                // add the missing redundant check in SwipeRefreshLayout.
                int pointerIndex = MotionEventCompat.findPointerIndex(event, activePointerId);
                if (pointerIndex < 0) {
                    Logger.warn(getClass().getSimpleName(), "Reached edge-case for swipe refresh layout's event handling.");
                    return false;
                }

                break;
            }
        }
        return super.onTouchEvent(event);
    }
}
