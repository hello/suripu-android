package is.hello.sense.flows.home.util;

import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.FeedPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.SoundsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TrendsPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class HomeViewPagerPresenterDelegate extends BaseViewPagerPresenterDelegate {
    public static final int SLEEP_ICON_KEY = 0;
    public final int TRENDS_ICON_KEY = 1;
    public final int FEED_ICON_KEY = 2;
    public final int SOUNDS_ICON_KEY = 3;
    public static final int CONDITIONS_ICON_KEY = 4;

    @NonNull
    @Override
    public HomeFragmentPagerAdapter.HomeItem[] getViewPagerItems() {
        return new HomeFragmentPagerAdapter.HomeItem[]{
                new HomeFragmentPagerAdapter.HomeItem(TimelinePagerPresenterFragment.class,
                                                      TimelinePagerPresenterFragment.class.getSimpleName(),
                                                      R.drawable.icon_sense_24,
                                                      R.drawable.icon_sense_24_duo),
                new HomeFragmentPagerAdapter.HomeItem(TrendsPresenterFragment.class,
                                                      TrendsPresenterFragment.class.getSimpleName(),
                                                      R.drawable.icon_trends_24,
                                                      R.drawable.icon_trends_24_duo),
                new HomeFragmentPagerAdapter.HomeItem(FeedPresenterFragment.class,
                                                      FeedPresenterFragment.class.getSimpleName(),
                                                      R.drawable.icon_insight_24,
                                                      R.drawable.icon_insight_24_duo),
                new HomeFragmentPagerAdapter.HomeItem(SoundsPresenterFragment.class,
                                                      SoundsPresenterFragment.class.getSimpleName(),
                                                      R.drawable.icon_sounds_24,
                                                      R.drawable.icon_sounds_24_duo),
                new HomeFragmentPagerAdapter.HomeItem(RoomConditionsPresenterFragment.class,
                                                      RoomConditionsPresenterFragment.class.getSimpleName(),
                                                      R.drawable.icon_sense_24,
                                                      R.drawable.icon_sense_24_duo)
        };
    }

    @Override
    public int getOffscreenPageLimit() {
        return 4;
    }
}
