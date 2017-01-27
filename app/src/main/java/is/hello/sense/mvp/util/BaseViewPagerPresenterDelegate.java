package is.hello.sense.mvp.util;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.util.NotTested;

/**
 * Custom delegates should extend this one and override it's methods.
 */
@NotTested
public abstract class BaseViewPagerPresenterDelegate implements ViewPagerPresenter {

    public final static int DEFAULT_STARTING_ITEM_POSITION = 0;
    public final static int DEFAULT_OFFSCREEN_PAGE_LIMIT = 2;

    @NonNull
    @Override
    public abstract StaticFragmentAdapter.Item[] getViewPagerItems();

    @Override
    public int getStartingItemPosition() {
        return DEFAULT_STARTING_ITEM_POSITION;
    }

    @Override
    public int getOffscreenPageLimit() {
        return DEFAULT_OFFSCREEN_PAGE_LIMIT;
    }
}
