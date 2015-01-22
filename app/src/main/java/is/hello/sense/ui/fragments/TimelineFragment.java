package is.hello.sense.ui.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.isAnimating;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, AdapterView.OnItemClickListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    @Inject DateFormatter dateFormatter;
    @Inject TimelinePresenter timelinePresenter;
    @Inject PreferencesPresenter preferences;

    private ListView listView;
    private TimelineSegmentAdapter segmentAdapter;

    private ImageButton menuButton;
    private ImageButton shareButton;
    private ImageButton smartAlarmButton;

    private ViewGroup headerView;
    private TextView dateText;
    private HeaderViewMode headerMode;
    private SummaryViewMode timelineSummary;

    private View timelineEventsHeader;


    public static TimelineFragment newInstance(@NonNull DateTime date) {
        TimelineFragment fragment = new TimelineFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date.withTimeAtStartOfDay());
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        timelinePresenter.setDate(getDate());
        addPresenter(timelinePresenter);

        this.segmentAdapter = new TimelineSegmentAdapter(getActivity(), dateFormatter);

        boolean defaultValue = DateFormat.is24HourFormat(getActivity());
        Observable<Boolean> use24HourTime = preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, defaultValue)
                                                       .subscribeOn(AndroidSchedulers.mainThread());
        track(use24HourTime.subscribe(segmentAdapter::setUse24Time));

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        listView.setOnItemClickListener(this);


        this.headerView = (ViewGroup) inflater.inflate(R.layout.sub_fragment_timeline_header, listView, false);

        this.dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));
        dateText.setOnClickListener(ignored -> ((HomeActivity) getActivity()).showTimelineNavigator(getDate()));

        this.timelineSummary = new SummaryViewMode(inflater, headerView);
        setHeaderMode(timelineSummary);

        ListViews.addHeaderView(listView, headerView, null, false);

        this.timelineEventsHeader = new View(getActivity());
        timelineEventsHeader.setBackgroundResource(R.drawable.background_timeline_top);
        timelineEventsHeader.setVisibility(View.INVISIBLE);
        timelineEventsHeader.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_outer));
        ListViews.addHeaderView(listView, timelineEventsHeader, null, false);

        View spacingFooter = new View(getActivity());
        spacingFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_outer));
        ListViews.addFooterView(listView, spacingFooter, null, false);


        this.menuButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_menu);
        Views.setSafeOnClickListener(menuButton, ignored -> {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.getSlidingLayersView().toggle();
        });

        this.shareButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_share);
        Views.setSafeOnClickListener(shareButton, ignored -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "http://hello.is");
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
        });

        this.smartAlarmButton = (ImageButton) view.findViewById(R.id.fragment_timeline_smart_alarm);
        Views.setSafeOnClickListener(smartAlarmButton, ignored -> {
            Intent intent = new Intent(getActivity(), SmartAlarmDetailActivity.class);
            intent.putExtras(SmartAlarmDetailActivity.getArguments(new Alarm(), SmartAlarmDetailActivity.INDEX_NEW));
            startActivity(intent);
        });

        listView.setAdapter(segmentAdapter);
        ListViews.setTouchAndScrollListener(listView, new TimelineScrollListener());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Timeline> boundMainTimeline = bind(timelinePresenter.mainTimeline);
        subscribe(boundMainTimeline, this::bindTimeline, this::timelineUnavailable);

        Observable<List<TimelineSegment>> segments = boundMainTimeline.map(timeline -> {
            if (timeline != null) {
                return timeline.getSegments();
            } else {
                return Collections.emptyList();
            }
        });
        subscribe(segments, segmentAdapter::bindSegments, segmentAdapter::handleError);

        Observable<CharSequence> renderedMessage = timelinePresenter.renderedTimelineMessage;
        bindAndSubscribe(renderedMessage, timelineSummary.messageText::setText, Functions.LOG_ERROR);
    }

    @Override
    public void onResume() {
        super.onResume();

        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));
    }

    public void onTransitionCompleted() {
        // This is the best place to fire animations.
    }


    //region Headers

    private void setHeaderMode(@NonNull HeaderViewMode headerMode) {
        if (this.headerMode != null) {
            headerView.removeView(this.headerMode.view);
        }

        this.headerMode = headerMode;
        headerView.addView(headerMode.view, 1);
    }

    //endregion

    public void bindTimeline(@Nullable Timeline timeline) {
        if (timeline != null) {
            timelineSummary.showSleepScore(timeline.getScore());

            if (timeline.getSegments().isEmpty()) {
                timelineEventsHeader.setVisibility(View.INVISIBLE);
            } else {
                timelineEventsHeader.setVisibility(View.VISIBLE);
            }
        } else {
            timelineSummary.showSleepScore(-1);
            timelineEventsHeader.setVisibility(View.INVISIBLE);
        }
    }

    public void timelineUnavailable(@Nullable Throwable e) {
        timelineSummary.presentError(e);
    }


    public DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }


    @Override
    public void onUserWillPullDownTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_open);
        dateText.setTextColor(getResources().getColor(R.color.text_dim));
        shareButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUserDidPushUpTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_closed);
        dateText.setTextColor(getResources().getColor(R.color.text_dark));
        shareButton.setVisibility(View.VISIBLE);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        TimelineSegment segment = (TimelineSegment) adapterView.getItemAtPosition(position);
        if (segment.getEventType() != null) {
            TimelineEventDialogFragment dialogFragment = TimelineEventDialogFragment.newInstance(segment);
            dialogFragment.show(getFragmentManager(), TimelineEventDialogFragment.TAG);
        }

        Analytics.trackEvent(Analytics.EVENT_TIMELINE_ACTION, Analytics.createProperties(Analytics.PROP_TIMELINE_ACTION, Analytics.PROP_TIMELINE_ACTION_TAP_EVENT));
    }


    //region Smart Alarm Button

    private void pushSmartAlarmOffScreen() {
        if (smartAlarmButton.getVisibility() == View.VISIBLE && !isAnimating(smartAlarmButton)) {
            int contentHeight = listView.getMeasuredHeight();

            animate(smartAlarmButton)
                    .y(contentHeight)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            smartAlarmButton.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }
    }

    private void pullSmartAlarmOnScreen() {
        if (smartAlarmButton.getVisibility() == View.INVISIBLE) {
            int contentHeight = listView.getMeasuredHeight();
            int buttonHeight = smartAlarmButton.getMeasuredHeight();

            smartAlarmButton.setVisibility(View.VISIBLE);

            animate(smartAlarmButton)
                    .y(contentHeight - buttonHeight)
                    .start();
        }
    }

    //endregion

    private class TimelineScrollListener extends ListViews.TouchAndScrollListener {
        @Override
        protected void onScrollStateChanged(@NonNull AbsListView absListView, int oldState, int newState) {
        }

        @Override
        public void onScroll(AbsListView listView, int firstVisiblePosition, int visibleItemCount, int totalItemCount) {
            if (firstVisiblePosition == 0 && ListViews.getEstimatedScrollY(listView) == 0) {
                pullSmartAlarmOnScreen();
            } else {
                pushSmartAlarmOffScreen();
            }
        }

        @Override
        protected void onTouchDown(@NonNull AbsListView absListView) {
        }

        @Override
        protected void onTouchUp(@NonNull AbsListView absListView) {
        }
    }



    class HeaderViewMode {
        final View view;

        HeaderViewMode(@NonNull View view) {
            this.view = view;
        }
    }

    class SummaryViewMode extends HeaderViewMode {
        final SleepScoreDrawable scoreGraph;
        final TextView scoreText;
        final TextView messageText;

        SummaryViewMode(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            super(inflater.inflate(R.layout.sub_fragment_timeline_summary, container, false));

            LinearLayout sleepScoreContainer = (LinearLayout) view.findViewById(R.id.fragment_timeline_sleep_score_chart);
            this.scoreText = (TextView) sleepScoreContainer.findViewById(R.id.fragment_timeline_sleep_score);
            this.messageText = (TextView) view.findViewById(R.id.fragment_timeline_message);

            this.scoreGraph = new SleepScoreDrawable(getResources());
            sleepScoreContainer.setBackground(scoreGraph);
        }


        void showSleepScore(int sleepScore) {
            if (sleepScore < 0) {
                scoreGraph.setFillColor(getResources().getColor(R.color.sleep_score_empty));
                scoreText.setText(R.string.missing_data_placeholder);
                scoreGraph.setValue(0);
            } else {
                if (sleepScore != scoreGraph.getValue()) {
                    ValueAnimator updateAnimation = ValueAnimator.ofInt(scoreGraph.getValue(), sleepScore);
                    Animations.Properties.createWithDelay(250).apply(updateAnimation);

                    ArgbEvaluator colorEvaluator = new ArgbEvaluator();
                    int startColor = scoreText.getCurrentTextColor();
                    int endColor = Styles.getSleepScoreColor(getActivity(), sleepScore);
                    updateAnimation.addUpdateListener(a -> {
                        Integer score = (Integer) a.getAnimatedValue();
                        int color = (int) colorEvaluator.evaluate(a.getAnimatedFraction(), startColor, endColor);

                        scoreGraph.setValue(score);
                        scoreGraph.setFillColor(color);

                        scoreText.setText(score.toString());
                        scoreText.setTextColor(color);
                    });

                    updateAnimation.start();
                }
            }
        }

        void presentError(Throwable e) {
            scoreGraph.setTrackColor(getResources().getColor(R.color.border));
            scoreGraph.setValue(0);
            scoreText.setText(R.string.missing_data_placeholder);
            scoreText.setTextColor(getResources().getColor(R.color.text_dark));

            if (e != null) {
                messageText.setText(getString(R.string.timeline_error_message, e.getMessage()));
            } else {
                messageText.setText(R.string.missing_data_placeholder);
            }
        }
    }
}
