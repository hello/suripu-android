package is.hello.sense.mvp.util;

import android.support.annotation.NonNull;

import is.hello.sense.mvp.adapters.StaticSubPresenterFragmentAdapter;

public interface ViewPagerPresenter {
    /**
     * @return List of items to be shown
     */
    @NonNull
    StaticSubPresenterFragmentAdapter.Item[] getViewPagerItems();

    /**
     * Initial Item that should be shown. Doesn't crash if out of bounds, instead will default to 0.
     *
     * @return index position in {@link #getViewPagerItems()}.
     */
    int getStartingItemPosition();
}
