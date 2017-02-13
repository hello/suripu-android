package is.hello.sense.flows.home.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.MonthTrendsFragment;
import is.hello.sense.flows.home.ui.fragments.QuarterTrendsFragment;
import is.hello.sense.flows.home.ui.fragments.WeekTrendsFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;

public class TrendsViewPagerPresenterDelegate extends BaseViewPagerPresenterDelegate {
    private final Resources resources;

    public TrendsViewPagerPresenterDelegate(@NonNull final Resources resources) {
        this.resources = resources;
    }

    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticFragmentAdapter.Item[]{
                new StaticFragmentAdapter.Item(WeekTrendsFragment.class,
                                               resources.getString(R.string.trend_time_scale_week)),
                new StaticFragmentAdapter.Item(MonthTrendsFragment.class,
                                               resources.getString(R.string.trend_time_scale_month)),
                new StaticFragmentAdapter.Item(QuarterTrendsFragment.class,
                                               resources.getString(R.string.trend_time_scale_quarter))
        };
    }
}
