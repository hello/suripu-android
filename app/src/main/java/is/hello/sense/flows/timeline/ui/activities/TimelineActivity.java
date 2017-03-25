package is.hello.sense.flows.timeline.ui.activities;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.timeline.TimelineModule;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionAppCompatActivity;
import is.hello.sense.ui.fragments.TimelineInfoFragment;
import is.hello.sense.ui.fragments.ZoomedOutTimelineFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

/**
 * Activity that handles displaying sleep score history
 */

public class TimelineActivity extends ScopedInjectionAppCompatActivity
        implements ZoomedOutTimelineFragment.OnTimelineDateSelectedListener {

    public static final String EXTRA_LOCAL_DATE = TimelineActivity.class.getSimpleName() + "EXTRA_LOCAL_DATE";
    public static final String EXTRA_TIMELINE = TimelineActivity.class.getSimpleName() + "EXTRA_TIMELINE";

    //todo should this activity be listening for this intent? How will it update the fragment's view pager later?
    private final BroadcastReceiver onTimeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final LocalDate newToday = DateFormatter.todayForTimeline();
        }
    };

    public static Intent getZoomedOutIntent(@NonNull final Context context,
                                            @NonNull final LocalDate startDate,
                                            @Nullable final Timeline timeline) {
        final Intent intent = new Intent(context, TimelineActivity.class);
        intent.putExtra(EXTRA_LOCAL_DATE, startDate);
        intent.putExtra(EXTRA_TIMELINE, timeline);

        return intent;
    }

    public static Intent getInfoIntent(@NonNull final Context context,
                                       @NonNull final Timeline timeline) {
        final Intent intent = new Intent(context, TimelineActivity.class);
        intent.putExtra(EXTRA_TIMELINE, timeline);

        return intent;
    }

    @Override
    protected List<Object> getModules() {
        return Collections.singletonList(new TimelineModule());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_navigation);
        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_LOCAL_DATE)) {
            showTimelineNavigator((LocalDate) intent.getSerializableExtra(EXTRA_LOCAL_DATE),
                                  (Timeline) intent.getSerializableExtra(EXTRA_TIMELINE));
        } else if (intent.hasExtra(EXTRA_TIMELINE)) {
            showTimelineInfo((Timeline) intent.getSerializableExtra(EXTRA_TIMELINE));
        }
        registerReceiver(onTimeChanged, new IntentFilter(Intent.ACTION_TIME_CHANGED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(onTimeChanged);
    }


    //region Timeline Navigation

    public void showTimelineNavigator(@NonNull final LocalDate startDate, @Nullable final Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_IN, null);

        final ZoomedOutTimelineFragment navigatorFragment =
                ZoomedOutTimelineFragment.newInstance(startDate, timeline);
        getFragmentManager()
                .beginTransaction()
                .replace(getRootContainerIdRes(),
                         navigatorFragment,
                         ZoomedOutTimelineFragment.TAG)
                .addToBackStack(ZoomedOutTimelineFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commitAllowingStateLoss();
    }

    @Override
    public void onTimelineSelected(@NonNull final LocalDate date, @Nullable final Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_OUT, null);

        final Intent timelineData = new Intent();
        timelineData.putExtra(EXTRA_LOCAL_DATE, date);
        timelineData.putExtra(EXTRA_TIMELINE, timeline);

        setResult(RESULT_OK, timelineData);
        finish();
    }

    //endregion

    //region TimelineInfo Breakdown

    public void showTimelineInfo(@NonNull final Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SLEEP_SCORE_BREAKDOWN, null);
        final TimelineInfoFragment infoOverlay =
                TimelineInfoFragment.newInstance(timeline,
                                                 R.id.place_holder_id); //any valid id could work here because not using animation effect
        infoOverlay.show(getFragmentManager(),
                         getRootContainerIdRes(),
                         TimelineInfoFragment.TAG);
    }

    //endregion

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public
    @IdRes
    final int getRootContainerIdRes() {
        return R.id.activity_fragment_navigation_container;
    }
}
