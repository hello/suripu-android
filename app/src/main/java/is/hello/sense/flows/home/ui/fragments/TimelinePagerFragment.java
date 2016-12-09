package is.hello.sense.flows.home.ui.fragments;

import android.app.Activity;
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
import is.hello.sense.flows.timeline.ui.activities.TimelineActivity;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.timeline.TimelineToolbar;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class TimelinePagerFragment extends InjectionFragment
        implements ViewPager.OnPageChangeListener,
        TimelineFragment.Parent {

    @Inject
    PreferencesInteractor preferences;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    TimelineInteractor timelineInteractor;


    private static final String KEY_LAST_UPDATED = TimelinePagerFragment.class.getSimpleName() + "KEY_LAST_UPDATED";

    private static final int ZOOMED_OUT_TIMELINE_REQUEST = 101;
    private TimelineToolbar toolbar;
    private ViewPager viewPager;
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
                TimelinePagerFragment.this.updateTitle(newToday);
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

        toolbar = (TimelineToolbar) view.findViewById(R.id.fragment_timeline_pager_toolbar);
        toolbar.setTitleDimmed(getUserVisibleHint() && isBacksideOpen());
        toolbar.setShareVisible(false);
        toolbar.setHistoryOnClickListener(this::onHistoryIconClicked);
        toolbar.setShareOnClickListener(this::onShareIconClicked);

        viewPager = (ExtendedViewPager) view.findViewById(R.id.fragment_timeline_view_pager);

        this.viewPagerAdapter = new TimelineFragmentAdapter(getChildFragmentManager(),
                                                            preferences.getAccountCreationDate());

        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(this);

        if(viewPager.getCurrentItem() == 0){
            jumpToLastNight(false);
        }

        getActivity().registerReceiver(onTimeChanged, new IntentFilter(Intent.ACTION_TIME_CHANGED));
        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable final Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null) {
            this.lastUpdated = savedInstanceState.getLong(KEY_LAST_UPDATED);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_LAST_UPDATED, lastUpdated);
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
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(Activity.RESULT_OK == resultCode){
            if(ZOOMED_OUT_TIMELINE_REQUEST == requestCode){
                final LocalDate date = (LocalDate) data.getSerializableExtra(TimelineActivity.EXTRA_LOCAL_DATE);
                final Timeline timeline = (Timeline) data.getSerializableExtra(TimelineActivity.EXTRA_TIMELINE);
                onTimelineSelected(date, timeline);
            }
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
        final LocalDate localDate = viewPagerAdapter.getItemDate(position);
        updateTitle(localDate);
        setShareVisible(timelineInteractor.hasValidTimeline(localDate));

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
        viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), animate);
    }

    public void onTimelineSelected(@NonNull final LocalDate date, @Nullable final Timeline timeline) {
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

    public void showTimelineNavigator(@NonNull final LocalDate date,
                                      @Nullable final Timeline timeline) {
        startActivityForResult(TimelineActivity.getZoomedOutIntent(getActivity(),
                                                                   date,
                                                                   timeline),
                               ZOOMED_OUT_TIMELINE_REQUEST);
    }

    private void onShareIconClicked(final View ignored) {
        final TimelineFragment current =  viewPagerAdapter.getCurrentTimeline();
        if(current != null){
            current.share();
        }
    }

    private void onHistoryIconClicked(final View ignored) {
        final TimelineFragment current =  viewPagerAdapter.getCurrentTimeline();
        if(current != null){
            current.dismissVisibleOverlaysAndDialogs();
            showTimelineNavigator(current.getDate(), current.getCachedTimeline());
        }
    }

    private String getTitle(@Nullable final LocalDate date){
        return dateFormatter.formatAsTimelineDate(date);
    }

    /**
     * @param date to use and update toolbar title display
     */
    public void updateTitle(@Nullable final LocalDate date) {
        if (toolbar != null) {
            toolbar.setTitle(getTitle(date));
        }
    }

    //region Timeline parent

    @Override
    public boolean isBacksideOpen() {
        return false;
    }

    @Override
    public int getTutorialContainerIdRes() {
        return R.id.fragment_timeline_pager_container;
    }

    @Override
    public void setShareVisible(final boolean visible) {
        if (toolbar != null) {
            toolbar.setShareVisible(visible);
        }
    }

    //endregion
}
