package is.hello.sense.flows.home.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.views.TimelinePagerView;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.mvp.presenters.ControllerPresenterFragment;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class TimelinePagerPresenterFragment extends ControllerPresenterFragment<TimelinePagerView>
        implements ViewPager.OnPageChangeListener,
        TimelineFragment.Parent,
        HomeActivity.ScrollUp {

    @Inject
    PreferencesInteractor preferences;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    TimelineInteractor timelineInteractor;

    private static final String KEY_LAST_UPDATED = TimelinePagerPresenterFragment.class.getSimpleName() + "KEY_LAST_UPDATED";
    private static final String KEY_LAST_ITEM = TimelinePagerPresenterFragment.class.getSimpleName() + "KEY_LAST_ITEM";

    public boolean shouldJumpToLastNightOnUserVisible = false;
    private final BroadcastReceiver onTimeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final LocalDate newToday = DateFormatter.todayForTimeline();
            final LocalDate selectedDate = TimelinePagerPresenterFragment.this.presenterView.getSelectedDate();
            if (newToday.isBefore(selectedDate)) {
                // ViewPager does not correctly shrink when the number of items in it
                // decrease, so we have to clear its adapter, update the adapter, then
                // re-set the adapter for the update to work correctly.
                //todo confirm above comment
                TimelinePagerPresenterFragment.this.presenterView.resetAdapterForLatestDate(newToday);
            } else {
                TimelinePagerPresenterFragment.this.presenterView.setLatestDate(newToday);
            }
        }
    };
    private int lastPagerScrollState = ViewPager.SCROLL_STATE_IDLE;
    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getName());
    private long lastUpdated = System.currentTimeMillis();


    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new TimelinePagerView(getActivity(),
                                                       createAdapter(),
                                                       this);
        }
    }

    private TimelineFragmentAdapter createAdapter() {
        return new TimelineFragmentAdapter(getChildFragmentManager(),
                                           this.preferences.getAccountCreationDate());
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            jumpToLastNight(false);
        } else {
            this.presenterView.setCurrentViewPagerItem(savedInstanceState.getInt(KEY_LAST_ITEM), false);
        }

        getActivity().registerReceiver(this.onTimeChanged, new IntentFilter(Intent.ACTION_TIME_CHANGED));
    }

    @Override
    public void onViewStateRestored(@Nullable final Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            this.lastUpdated = savedInstanceState.getLong(KEY_LAST_UPDATED);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_LAST_UPDATED, this.lastUpdated);

        outState.putInt(KEY_LAST_ITEM, this.presenterView == null ? 0 : this.presenterView.getCurrentItem());
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((System.currentTimeMillis() - this.lastUpdated) > Constants.STALE_INTERVAL_MS) {
            if (isCurrentFragmentLastNight()) {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, reloading.");
                final TimelineFragment fragment = this.presenterView.getCurrentTimeline();
                if (fragment != null) {
                    fragment.update();
                }
            } else {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
                this.presenterView.setCurrentItemToLastNight(false);
            }

            this.lastUpdated = System.currentTimeMillis();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        if (isVisible) {
            if (shouldJumpToLastNightOnUserVisible) {
                shouldJumpToLastNightOnUserVisible = false;
                jumpToLastNight(false);
            }
            final TimelineFragment timelineFragment = this.presenterView.getCurrentTimeline();
            if (timelineFragment != null) {
                timelineFragment.setUserVisibleHint(true);
            }
        } else {
            final TimelineFragment timelineFragment = this.presenterView.getCurrentTimeline();
            if (timelineFragment != null) {
                timelineFragment.dismissVisibleOverlaysAndDialogs();
                timelineFragment.setUserVisibleHint(false);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(onTimeChanged);
        timelineInteractor.clearCache();
    }

    //region Fragment Adapter

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(final int position) {

    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        if (lastPagerScrollState == ViewPager.SCROLL_STATE_IDLE &&
                state != ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.beginAnimation("Timeline swipe");

            final TimelineFragment currentFragment = this.presenterView.getCurrentTimeline();
            if (currentFragment != null) {
                currentFragment.onSwipeBetweenDatesStarted();
            }
        } else if (lastPagerScrollState != ViewPager.SCROLL_STATE_IDLE &&
                state == ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.endAnimation("Timeline swipe");

            Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_SWIPE, null);
        }
        this.lastPagerScrollState = state;
    }

    //endregion

    public boolean isCurrentFragmentLastNight() {
        final TimelineFragment currentFragment = this.presenterView.getCurrentTimeline();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }

    public void jumpToLastNight(final boolean animate) {
        this.presenterView.setCurrentItemToLastNight(animate);
    }


    //region Timeline parent

    @Override
    public int getTutorialContainerIdRes() {
        return R.id.fragment_timeline_pager_container;
    }

    @Override
    public void jumpToLastNight() {
        shouldJumpToLastNightOnUserVisible = true;
    }

    @Override
    public void jumpTo(@NonNull final LocalDate date, @Nullable final Timeline timeline) {
        if (presenterView == null) {
            return;
        }
        this.presenterView.jumpToDate(date, timeline);
    }

    //endregion
    //region ScrollUp
    @Override
    public void scrollUp() {
        if (presenterView == null) {
            return;
        }
        this.presenterView.scrollUp();
    }

    //endregion
}
