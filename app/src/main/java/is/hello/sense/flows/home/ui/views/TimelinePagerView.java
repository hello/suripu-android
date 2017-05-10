package is.hello.sense.flows.home.ui.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;

import org.joda.time.LocalDate;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.ui.widget.ExtendedViewPager;

@SuppressLint("ViewConstructor")
public class TimelinePagerView extends PresenterView {
    private final ExtendedViewPager viewPager;
    private final TimelineFragmentAdapter viewPagerAdapter;

    public TimelinePagerView(@NonNull final Activity activity,
                             @NonNull final TimelineFragmentAdapter viewPagerAdapter,
                             @NonNull final ViewPager.OnPageChangeListener listener) {
        super(activity);
        this.viewPager = (ExtendedViewPager) findViewById(R.id.fragment_timeline_view_pager);
        this.viewPagerAdapter = viewPagerAdapter;
        this.viewPager.setAdapter(viewPagerAdapter);
        this.viewPager.addOnPageChangeListener(listener);

    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_timeline_pager;
    }

    @Override
    public void releaseViews() {
        this.viewPager.clearOnPageChangeListeners();
        this.viewPager.setAdapter(null);

    }
    //endregion


    public LocalDate getSelectedDate() {
        return this.viewPagerAdapter.getItemDate(getCurrentItem());
    }

    public void resetAdapterForLatestDate(@NonNull final LocalDate newToday) {
        this.viewPager.setAdapter(null);
        setLatestDate(newToday);
        this.viewPager.setAdapter(this.viewPagerAdapter);
        setCurrentItemToLastNight(false);
    }

    public void setCurrentViewPagerItem(final int item,
                                        final boolean withSmoothScroll) {
        this.viewPager.setCurrentItem(item, withSmoothScroll);
    }

    public void setLatestDate(@NonNull final LocalDate date) {
        this.viewPagerAdapter.setLatestDate(date);
    }

    public int getCurrentItem() {
        return viewPager.getCurrentItem();
    }

    public void setCurrentItemToLastNight(final boolean withSmoothScroll) {
        setCurrentViewPagerItem(this.viewPagerAdapter.getLastNight(), withSmoothScroll);
    }

    @Nullable
    public TimelineFragment getCurrentTimeline() {
        return viewPagerAdapter.getCurrentTimeline();
    }

    public void jumpToDate(@NonNull final LocalDate date,
                           @Nullable final Timeline timeline) {
        final int datePosition = this.viewPagerAdapter.getDatePosition(date);
        if (datePosition != getCurrentItem()) {
            this.viewPagerAdapter.setCachedTimeline(timeline);
            setCurrentViewPagerItem(datePosition, false);
        } else {
            final TimelineFragment currentFragment = this.viewPagerAdapter.getCurrentTimeline();
            if (currentFragment != null) {
                currentFragment.scrollToTop();
            }
        }
    }

    public void scrollUp() {
        if (this.viewPager.getCurrentItem() == this.viewPagerAdapter.getLastNight()) {
            final TimelineFragment fragment = getCurrentTimeline();
            if (fragment != null) {
                fragment.scrollToTop();
            }
        } else {
            setCurrentItemToLastNight(true);
        }

    }

    public void enableScrolling(final boolean enable) {
        this.viewPager.setScrollingEnabled(enable);
    }


}
