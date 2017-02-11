package is.hello.sense.flows.home.util;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.ui.fragments.FeedPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.SoundsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TrendsPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class HomeViewPagerPresenterDelegate extends BaseViewPagerPresenterDelegate {
    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticFragmentAdapter.Item[]{
                new StaticFragmentAdapter.Item(TimelinePagerPresenterFragment.class, TimelinePagerPresenterFragment.class.getSimpleName()),
                new StaticFragmentAdapter.Item(TrendsPresenterFragment.class, TrendsPresenterFragment.class.getSimpleName()),
                new StaticFragmentAdapter.Item(FeedPresenterFragment.class, FeedPresenterFragment.class.getSimpleName()),
                new StaticFragmentAdapter.Item(SoundsPresenterFragment.class, SoundsPresenterFragment.class.getSimpleName()),
                new StaticFragmentAdapter.Item(RoomConditionsPresenterFragment.class, RoomConditionsPresenterFragment.class.getSimpleName())
        };
    }

    @Override
    public int getOffscreenPageLimit() {
        return 4;
    }
}
