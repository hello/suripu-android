package is.hello.sense.ui.widget.util;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;

import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;

/**
 * Implements {@link ViewPager.OnPageChangeListener} to provide a consistent interface
 * for tracking swipe interactions within a {@link ViewPager} instance.
 */
public final class OnViewPagerChangeAdapter implements ViewPager.OnPageChangeListener {
    private final ViewPager viewPager;
    private final Listener listener;

    /**
     * The last known scroll [item] position of the view pager.
     */
    private int lastScrollPosition;

    /**
     * The last known scroll state of the view pager.
     */
    private int lastScrollState = SCROLL_STATE_IDLE;


    public OnViewPagerChangeAdapter(@NonNull ViewPager viewPager, @NonNull Listener listener) {
        this.viewPager = viewPager;
        this.listener = listener;

        this.lastScrollPosition = viewPager.getCurrentItem();
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // If the user swipes very quickly, it's possible that we'll
        // miss a completed event. This catches that edge case.
        if (lastScrollState != SCROLL_STATE_IDLE && position != lastScrollPosition) {
            listener.onPageChangeCompleted(position);
        }

        // ViewPager has a bug wherein `positionOffset` can be greater than 1f.
        final float normalizedOffset = Math.min(1f, positionOffset);
        listener.onPageChangeScrolled(position, normalizedOffset);

        this.lastScrollPosition = position;
    }

    @Override
    public void onPageSelected(int position) {
        if (lastScrollState == SCROLL_STATE_IDLE) {
            if (ViewCompat.isLaidOut(viewPager)) {
                listener.onPageChangeCompleted(position);
            } else {
                viewPager.requestLayout();
                viewPager.post(() -> listener.onPageChangeCompleted(position));
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == SCROLL_STATE_IDLE && lastScrollState != SCROLL_STATE_IDLE) {
            listener.onPageChangeCompleted(viewPager.getCurrentItem());
        }

        this.lastScrollState = state;
    }


    public interface Listener {
        /**
         * Called when the current page is scrolled, either programmatically,
         * or via user interaction. Will not be called if the current page is
         * changed programmatically without animation.
         *
         * @see ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)
         */
        void onPageChangeScrolled(int position, float offset);

        /**
         * Called when the view pager has completed a page change. This can be
         * in response to a programmatic change, or a user interaction. This method
         * is guaranteed to be called <em>after</em> {@link #onPageChangeScrolled(int, float)}.
         * @param position  The new page selected.
         */
        void onPageChangeCompleted(int position);
    }
}
