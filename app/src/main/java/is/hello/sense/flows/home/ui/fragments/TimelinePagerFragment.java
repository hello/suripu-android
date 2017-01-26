package is.hello.sense.flows.home.ui.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.mvp.util.ViewPagerPresenterChild;
import is.hello.sense.mvp.util.ViewPagerPresenterChildDelegate;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class TimelinePagerFragment extends InjectionFragment
        implements ViewPager.OnPageChangeListener,
        TimelineFragment.Parent,
        HomeActivity.ScrollUp,
        StaticFragmentAdapter.Controller {

    @Inject
    PreferencesInteractor preferences;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    TimelineInteractor timelineInteractor;

    private static final String KEY_LAST_UPDATED = TimelinePagerFragment.class.getSimpleName() + "KEY_LAST_UPDATED";
    private static final String KEY_LAST_ITEM = TimelinePagerFragment.class.getSimpleName() + "KEY_LAST_ITEM";

    private ViewPager viewPager;
    public boolean shouldJumpToLastNightOnUserVisible = false;
    private TimelineFragmentAdapter viewPagerAdapter;
    private final BroadcastReceiver onTimeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final LocalDate newToday = DateFormatter.todayForTimeline();
            final LocalDate selectedDate = viewPagerAdapter.getItemDate(viewPager.getCurrentItem());
            if (newToday.isBefore(selectedDate)) {
                // ViewPager does not correctly shrink when the number of items in it
                // decrease, so we have to clear its adapter, update the adapter, then
                // re-set the adapter for the update to work correctly.
                viewPager.setAdapter(null);
                viewPagerAdapter.setLatestDate(newToday);
                viewPager.setAdapter(viewPagerAdapter);
                viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), false);
            } else {
                viewPagerAdapter.setLatestDate(newToday);
            }
        }
    };
    private int lastPagerScrollState = ViewPager.SCROLL_STATE_IDLE;
    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getName());
    private long lastUpdated = System.currentTimeMillis();


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_timeline_pager, container, false);
        viewPager = (ExtendedViewPager) view.findViewById(R.id.fragment_timeline_view_pager);
        this.viewPagerAdapter = new TimelineFragmentAdapter(getChildFragmentManager(),
                                                            preferences.getAccountCreationDate());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(this);
        if (savedInstanceState == null) {
            jumpToLastNight(false);
        } else {
            viewPager.setCurrentItem(savedInstanceState.getInt(KEY_LAST_ITEM), false);
        }

        getActivity().registerReceiver(onTimeChanged, new IntentFilter(Intent.ACTION_TIME_CHANGED));
        return view;
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
        outState.putLong(KEY_LAST_UPDATED, lastUpdated);
        outState.putInt(KEY_LAST_ITEM, viewPager == null ? 0 : viewPager.getCurrentItem());
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS) {
            if (isCurrentFragmentLastNight()) {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, reloading.");
                final TimelineFragment fragment =
                        (TimelineFragment) viewPagerAdapter.getCurrentFragment();
                if (fragment != null) {
                    fragment.update();
                }
            } else {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
                viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), false);
            }

            this.lastUpdated = System.currentTimeMillis();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean hasPresenterView() {
        debugLog("Has Presenter View  = " + (viewPagerAdapter != null));
        return viewPagerAdapter != null;
    }

    @Override
    public void isVisibleToUser() {
        debugLog("isVisibleToUser");
        if (shouldJumpToLastNightOnUserVisible) {
            shouldJumpToLastNightOnUserVisible = false;
            jumpToLastNight(false);
        }
        final TimelineFragment timelineFragment = viewPagerAdapter.getCurrentTimeline();
        if (timelineFragment != null) {
            timelineFragment.setUserVisibleHint(true);
        }
    }

    @Override
    public void isInvisibleToUser() {
        debugLog("isInvisibleToUser");
        final TimelineFragment timelineFragment = viewPagerAdapter.getCurrentTimeline();
        if (timelineFragment != null) {
            timelineFragment.dismissVisibleOverlaysAndDialogs();
            timelineFragment.setUserVisibleHint(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getActivity().unregisterReceiver(onTimeChanged);
        viewPager.removeOnPageChangeListener(this);
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

            final TimelineFragment currentFragment =
                    (TimelineFragment) viewPagerAdapter.getCurrentFragment();
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
        final TimelineFragment currentFragment =
                (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }

    public void jumpToLastNight(final boolean animate) {
        if (viewPager == null || viewPagerAdapter == null) {
            return;
        }
        viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), animate);
    }


    //region Timeline parent

    @Override
    public int getTutorialContainerIdRes() {
        return R.id.fragment_timeline_pager_container;
    }

    @Override
    public int getTooltipOverlayContainerIdRes() {
        return R.id.fragment_timeline_view_pager;
    }

    @Override
    public void jumpToLastNight() {
        shouldJumpToLastNightOnUserVisible = true;
    }

    @Override
    public void jumpTo(@NonNull final LocalDate date, @Nullable final Timeline timeline) {
        final int datePosition = viewPagerAdapter.getDatePosition(date);
        if (datePosition != viewPager.getCurrentItem()) {
            viewPagerAdapter.setCachedTimeline(timeline);
            viewPager.setCurrentItem(datePosition, false);
        } else {
            final TimelineFragment currentFragment = viewPagerAdapter.getCurrentTimeline();
            if (currentFragment != null) {
                currentFragment.scrollToTop();
            }
        }
    }

    //endregion
    //region ScrollUp
    @Override
    public void scrollUp() {
        if (viewPager == null || viewPagerAdapter == null) {
            return;
        }
        if (viewPager.getCurrentItem() == viewPagerAdapter.getLastNight()) {
            final Fragment fragment = viewPagerAdapter.getCurrentFragment();
            if (fragment instanceof TimelineFragment) {
                ((TimelineFragment) fragment).scrollToTop();
            }
        } else {
            jumpToLastNight(true);
        }
    }

    //endregion
}
