package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Field;
import java.util.ArrayList;

import is.hello.sense.functional.Lists;
import is.hello.sense.util.Logger;

/**
 * Works around the open following open issues:
 * <ol>
 * <li>https://code.google.com/p/android/issues/detail?id=18990</li>
 * <li>https://code.google.com/p/android/issues/detail?id=66620</li>
 * </ol>
 */
public class ExtendedViewPager extends ViewPager {
    private
    @Nullable
    Field mItemsField;
    private boolean scrollingEnabled = true;
    /**
     * Determines if view pager will animate between children when different child selected
     */
    private boolean smoothScroll = true;

    public ExtendedViewPager(@NonNull final Context context) {
        super(context);
    }

    public ExtendedViewPager(@NonNull final Context context,
                             @Nullable final AttributeSet attrs) {
        super(context, attrs);

        // See #mItemsIsEmpty()
        try {
            this.mItemsField = ViewPager.class.getDeclaredField("mItems");
            mItemsField.setAccessible(true);
        } catch (final NoSuchFieldException e) {
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
            } catch (final IllegalAccessException e) {
                Logger.error(getClass().getSimpleName(), "Could not access `mItems` field", e);
                return false;
            }
        } else {
            return false;
        }
    }


    @Override
    public boolean onInterceptTouchEvent(final MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mItemsIsEmpty()) {
            return false;
        }

        try {
            return scrollingEnabled && super.onInterceptTouchEvent(event);
        } catch (final IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mItemsIsEmpty()) {
            return false;
        }

        try {
            return scrollingEnabled && super.onTouchEvent(event);
        } catch (final IllegalArgumentException e) {
            Logger.warn(getClass().getSimpleName(), "Swallowing illegal argument exception", e);
            return false;
        }
    }

    @Override
    public boolean canScrollHorizontally(final int direction) {
        return scrollingEnabled && super.canScrollHorizontally(direction);
    }

    @Override
    public void setCurrentItem(final int item) {
        super.setCurrentItem(item, this.smoothScroll);
    }

    @Override
    public void setCurrentItem(final int item, final boolean ignoredSmoothScroll) {
        super.setCurrentItem(item, this.smoothScroll);
    }

    public void setScrollingEnabled(final boolean swipingEnabled) {
        this.scrollingEnabled = swipingEnabled;
    }

    /**
     * @param smoothScroll setting to false prevents using any animation between pages entirely.
     *                     However, this breaks using {@link FadePageTransformer}
     */
    public void setSmoothScroll(final boolean smoothScroll){
        this.smoothScroll = smoothScroll;
    }

    public void setFadePageTransformer(final boolean fade) {
        if (fade) {
            this.setPageTransformer(false, new FadePageTransformer());
        } else {
            this.setPageTransformer(false, null);
        }
    }

    public static class FadePageTransformer implements ViewPager.PageTransformer {

        public FadePageTransformer() {
        }

        @Override
        public void transformPage(final View view, final float position) {
            view.setTranslationX(view.getWidth() * -position);

            if (position <= -1.0F || position >= 1.0F) {
                view.setAlpha(0.0F);
                view.setVisibility(INVISIBLE);
            } else if (position == 0.0F) {
                view.setAlpha(1.0F);
                view.setVisibility(VISIBLE);
            } else {
                // position is between -1.0F & 0.0F OR 0.0F & 1.0F
                view.setAlpha(1.0F - Math.abs(position));
            }
        }
    }
}
