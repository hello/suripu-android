package is.hello.sense.ui.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.Gravity;
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
import is.hello.sense.api.model.PreSleepInsight;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.dialogs.WelcomeDialog;
import is.hello.sense.ui.widget.IconAndTextDrawable;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.TimelineTabsDrawable;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Markdown;
import is.hello.sense.util.SafeOnClickListener;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.isAnimating;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, AdapterView.OnItemClickListener, SelectorLinearLayout.OnSelectionChangedListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    @Inject DateFormatter dateFormatter;
    @Inject TimelinePresenter timelinePresenter;
    @Inject PreferencesPresenter preferences;
    @Inject Markdown markdown;

    private ListView listView;
    private TimelineSegmentAdapter segmentAdapter;

    private ImageButton menuButton;
    private ImageButton shareButton;
    private ImageButton smartAlarmButton;

    private ViewGroup headerView;
    private TextView dateText;
    private HeaderViewMode headerMode;

    private ScoreViewMode timelineScore;
    private BeforeSleepHeaderMode beforeSleep;

    private View timelineEventsHeader;

    private boolean showTimelineWelcomeOnLayersUp = false;


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

        SelectorLinearLayout headerModeSelector = (SelectorLinearLayout) headerView.findViewById(R.id.sub_fragment_timeline_header_mode);
        headerModeSelector.setBackground(new TimelineTabsDrawable(getResources()));
        headerModeSelector.setOnSelectionChangedListener(this);
        IconAndTextDrawable.replaceBuiltInDrawing(headerModeSelector.getToggleButtons());
        headerModeSelector.setSelectedIndex(0);

        this.timelineScore = new ScoreViewMode(inflater, headerView);
        this.beforeSleep = new BeforeSleepHeaderMode(inflater, headerView);

        setHeaderMode(timelineScore);

        ListViews.addHeaderView(listView, headerView, null, false);


        this.timelineEventsHeader = new View(getActivity());
        timelineEventsHeader.setBackgroundResource(R.drawable.background_timeline_top);
        timelineEventsHeader.setVisibility(View.INVISIBLE);
        timelineEventsHeader.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_outer));
        ListViews.addHeaderView(listView, timelineEventsHeader, null, false);


        this.menuButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_menu);
        Views.setSafeOnClickListener(menuButton, ignored -> {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.getSlidingLayersView().toggle();
        });

        this.shareButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_share);
        Views.setSafeOnClickListener(shareButton, ignored -> {
            Analytics.trackEvent(Analytics.Timeline.EVENT_SHARE, null);

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
        bindAndSubscribe(renderedMessage, timelineScore.messageText::setText, Functions.LOG_ERROR);
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
        if (this.headerMode == headerMode) {
            return;
        }

        if (this.headerMode != null) {
            headerView.removeView(this.headerMode.view);
        }

        this.headerMode = headerMode;
        headerView.addView(headerMode.view, 2); // Between the Spaces
    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        switch (newSelectionIndex) {
            case 0:
                setHeaderMode(timelineScore);
                break;
            case 1:
                setHeaderMode(beforeSleep);
                break;
            default:
                break;
        }
    }

    //endregion


    public void bindTimeline(@Nullable Timeline timeline) {
        if (timeline != null) {
            timelineScore.showSleepScore(timeline.getScore());

            if (timeline.getSegments().isEmpty()) {
                timelineEventsHeader.setVisibility(View.INVISIBLE);
            } else {
                timelineEventsHeader.setVisibility(View.VISIBLE);

                HomeActivity activity = (HomeActivity) getActivity();
                if (activity.getSlidingLayersView().isOpen()) {
                    this.showTimelineWelcomeOnLayersUp = true;
                } else {
                    WelcomeDialog.showIfNeeded(activity, R.xml.welcome_dialog_timeline);
                }
            }
        } else {
            timelineScore.showSleepScore(-1);
            timelineEventsHeader.setVisibility(View.INVISIBLE);
        }

        beforeSleep.bindTimeline(timeline);
    }

    public void timelineUnavailable(@Nullable Throwable e) {
        timelineScore.presentError(e);
        beforeSleep.presentError(e);
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
            Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_EVENT_TAPPED, null);

            TimelineEventDialogFragment dialogFragment = TimelineEventDialogFragment.newInstance(segment);
            dialogFragment.show(getFragmentManager(), TimelineEventDialogFragment.TAG);
        }

        Analytics.trackEvent(Analytics.Timeline.EVENT_TAP, null);
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

        if (showTimelineWelcomeOnLayersUp) {
            WelcomeDialog.showIfNeeded(getActivity(), R.xml.welcome_dialog_timeline);
            this.showTimelineWelcomeOnLayersUp = false;
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



    //region Header Modes

    class HeaderViewMode {
        final View view;

        HeaderViewMode(@LayoutRes int layoutRes, @NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            this.view = inflater.inflate(layoutRes, container, false);
        }
    }

    class ScoreViewMode extends HeaderViewMode {
        final SleepScoreDrawable scoreGraph;
        final TextView scoreText;
        final TextView messageText;

        ScoreViewMode(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            super(R.layout.sub_fragment_timeline_score, inflater, container);

            LinearLayout sleepScoreContainer = (LinearLayout) view.findViewById(R.id.fragment_timeline_sleep_score_chart);
            this.scoreText = (TextView) sleepScoreContainer.findViewById(R.id.fragment_timeline_sleep_score);
            this.messageText = (TextView) view.findViewById(R.id.fragment_timeline_message);
            Views.makeTextViewLinksClickable(messageText);

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

    class BeforeSleepHeaderMode extends HeaderViewMode {
        final LinearLayout container;
        final LayoutInflater inflater;

        BeforeSleepHeaderMode(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            super(R.layout.sub_fragment_timeline_before_sleep, inflater, container);

            this.container = (LinearLayout) view.findViewById(R.id.sub_fragment_timeline_before_sleep_container);
            this.inflater = inflater;
        }


        TextView inflateNewItem() {
            return (TextView) inflater.inflate(R.layout.item_before_sleep_insight, container, false);
        }

        void bindTimeline(@Nullable Timeline timeline) {
            container.removeAllViews();

            if (timeline != null && !Lists.isEmpty(timeline.getPreSleepInsights())) {
                Context context = getActivity();
                View.OnClickListener onClick = new SafeOnClickListener(ignored -> {
                    Analytics.trackEvent(Analytics.Timeline.EVENT_BEFORE_SLEEP_EVENT_TAPPED, null);
                });
                for (PreSleepInsight insight : timeline.getPreSleepInsights()) {
                    TextView text = inflateNewItem();
                    text.setCompoundDrawablesRelativeWithIntrinsicBounds(insight.getIcon(context), null, null, null);
                    text.setText(insight.getMessage());
                    text.setOnClickListener(onClick);
                    markdown.renderInto(text, insight.getMessage());
                    container.addView(text);
                }
            } else {
                TextView text = inflateNewItem();
                text.setGravity(Gravity.CENTER);
                text.setText(R.string.placeholder_no_before_sleep_insights);
                container.addView(text);
            }
        }

        void presentError(@Nullable Throwable e) {
            container.removeAllViews();

            TextView text = inflateNewItem();
            text.setGravity(Gravity.CENTER);
            if (e != null) {
                text.setText(getString(R.string.timeline_error_message, e.getMessage()));
            } else {
                text.setText(R.string.missing_data_placeholder);
            }
            container.addView(text);
        }
    }

    //endregion
}
