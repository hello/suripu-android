package is.hello.sense.flows.home.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.MonthTrendsFragment;
import is.hello.sense.flows.home.ui.fragments.QuarterTrendsFragment;
import is.hello.sense.flows.home.ui.fragments.WeekTrendsFragment;
import is.hello.sense.mvp.adapters.StaticSubPresenterFragmentAdapter;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class TrendsViewPagerPresenterDelegate extends BaseViewPagerPresenterDelegate {
    private final Resources resources;

    public TrendsViewPagerPresenterDelegate(@NonNull final Resources resources) {
        this.resources = resources;
    }

    @NonNull
    @Override
    public StaticSubPresenterFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticSubPresenterFragmentAdapter.Item[]{
                new StaticSubPresenterFragmentAdapter.Item(WeekTrendsFragment.class,
                                                           resources.getString(R.string.trend_time_scale_week)),
                new StaticSubPresenterFragmentAdapter.Item(MonthTrendsFragment.class,
                                                           resources.getString(R.string.trend_time_scale_month)),
                new StaticSubPresenterFragmentAdapter.Item(QuarterTrendsFragment.class,
                                                           resources.getString(R.string.trend_time_scale_quarter))
        };
    }
}
