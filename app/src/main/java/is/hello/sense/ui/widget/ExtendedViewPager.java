package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;

import is.hello.sense.functional.Lists;
import is.hello.sense.util.Logger;

/**
 * Works around the open following open issues:
 * <ol>
 *     <li>https://code.google.com/p/android/issues/detail?id=18990</li>
 *     <li>https://code.google.com/p/android/issues/detail?id=66620</li>
 * </ol>
 */
public class ExtendedViewPager extends ViewPager {
    private @Nullable Field mItemsField;
    private boolean scrollingEnabled = true;

    public ExtendedViewPager(@NonNull Context context) {
        super(context);
    }

    public ExtendedViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // See #mItemsIsEmpty()
        try {
            this.mItemsField = ViewPager.class.getDeclaredField("mItems");
            mItemsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Logger.error(getClass().getSimpleName(), "Could not get `mItems` field", e);
        }
    }

    /**
     * There appears to be a race condition inside of {@code ViewPager} where its adapter can
     * have items, and before the {@code ViewPager} is able to populate its internal items list,
     * the user gets in some touch events. This will result in {@code IndexOutOfBoundsException}s.
     */
    private boolean mItemsIsEmpty() {
        if (mItemsField != null) {
            try {
                final ArrayList<?> mItems = (ArrayList<?>) mItemsField.get(this);
                return Lists.isEmpty(mItems);
            } catch (IllegalAccessException e) {
                Logger.error(getClass().getSimpleName(), "Could not access `mItems` field", e);
                return false;
            }
        } else {
            return false;
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mItemsIsEmpty()) {
            return false;
        }

        try {
            return scrollingEnabled && super.onInterceptTouchEvent(event);
        } catch (IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mItemsIsEmpty()) {
            return false;
        }

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
