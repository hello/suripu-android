package is.hello.sense.mvp.util;

import android.support.annotation.NonNull;

import is.hello.sense.ui.adapter.StaticFragmentAdapter;

public interface ViewPagerPresenter {
    /**
     * @return List of items to be shown
     */
    @NonNull
    StaticFragmentAdapter.Item[] getViewPagerItems();

    /**
     * Initial Item that should be shown. Doesn't crash if out of bounds, instead will default to 0.
     *
     * @return index position in {@link #getViewPagerItems()}.
     */
    int getStartingItemPosition();
}
