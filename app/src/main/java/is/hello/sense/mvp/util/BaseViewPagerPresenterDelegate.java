package is.hello.sense.mvp.util;

import android.support.annotation.NonNull;

import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.util.NotTested;

/**
 * Custom delegates should extend this one and override it's methods.
 */
@NotTested
public abstract class BaseViewPagerPresenterDelegate implements ViewPagerPresenter {

    public final static int DEFAULT_OFFSCREEN_PAGE_LIMIT = 2;

    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticFragmentAdapter.Item[0];
    }

    @Override
    public int getStartingItemPosition() {
        return 0;
    }

    @Override
    public int getOffscreenPageLimit() {
        return DEFAULT_OFFSCREEN_PAGE_LIMIT;
    }
}
