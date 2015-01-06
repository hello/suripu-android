package is.hello.sense.ui.fragments;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.PreSleepInsight;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.TimestampTextView;
import is.hello.sense.ui.widget.graphing.PieGraphView;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Markdown;
import is.hello.sense.util.SafeOnClickListener;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.isAnimating;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, AdapterView.OnItemClickListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    private static final int MESSAGE_FADE_OUT = 0;
    private static final long MESSAGE_FADE_OUT_DELAY = 150;

    @Inject DateFormatter dateFormatter;
    @Inject TimelinePresenter timelinePresenter;
    @Inject Markdown markdown;

    private ListView listView;
    private TimelineSegmentAdapter segmentAdapter;

    private TimestampTextView timeScrubber;
    private float scrollContentAperture;
    private float timeScrubberTrackHeight;

    private ImageButton menuButton;
    private ImageButton shareButton;
    private ImageButton smartAlarmButton;

    private TextView dateText;
    private PieGraphView scoreGraph;
    private TextView scoreText;
    private TextView messageText;

    private TextView timelineEventsHeader;
    private LinearLayout beforeSleepHeader;
    private LinearLayout beforeSleepItemContainer;
    private TextView beforeSleepMessage;
    private int selectedBeforeSleepInsight = -1;

    private final Handler fadeOutHandler = new Handler(message -> {
        fadeOutTimeScrubber();
        return true;
    });


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

        this.segmentAdapter = new TimelineSegmentAdapter(getActivity());

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.timeScrubber = (TimestampTextView) view.findViewById(R.id.fragment_timeline_scrubber);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        listView.setOnItemClickListener(this);


        View headerView = inflater.inflate(R.layout.sub_fragment_timeline_header, listView, false);

        this.scoreGraph = (PieGraphView) headerView.findViewById(R.id.fragment_timeline_sleep_score_chart);
        this.scoreText = (TextView) headerView.findViewById(R.id.fragment_timeline_sleep_score);
        this.messageText = (TextView) headerView.findViewById(R.id.fragment_timeline_message);

        this.dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));
        dateText.setOnClickListener(ignored -> ((HomeActivity) getActivity()).showTimelineNavigator(getDate()));

        listView.addHeaderView(headerView, null, false);

        this.beforeSleepHeader = (LinearLayout) inflater.inflate(R.layout.sub_fragment_before_sleep, listView, false);
        this.beforeSleepItemContainer = (LinearLayout) beforeSleepHeader.findViewById(R.id.fragment_timeline_before_sleep_container);
        this.beforeSleepMessage = (TextView) beforeSleepHeader.findViewById(R.id.fragment_timeline_before_sleep_message);
        Animations.Properties.DEFAULT.apply(beforeSleepHeader.getLayoutTransition(), false);
        beforeSleepHeader.setVisibility(View.GONE);
        listView.addHeaderView(beforeSleepHeader, null, false);

        this.timelineEventsHeader = (TextView) inflater.inflate(R.layout.item_section_header, listView, false);
        timelineEventsHeader.setText(R.string.title_events_timeline);
        timelineEventsHeader.setVisibility(View.INVISIBLE);
        listView.addHeaderView(timelineEventsHeader, null, false);

        View spacingFooter = new View(getActivity());
        spacingFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_outer));
        listView.addFooterView(spacingFooter, null, false);


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
            intent.putExtras(SmartAlarmDetailActivity.getArguments(new SmartAlarm(), SmartAlarmDetailActivity.INDEX_NEW));
            startActivity(intent);
        });

        // Always do this after adding headers and footer views,
        // we have to support Android versions under 4.4 KitKat.
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
        bindAndSubscribe(renderedMessage, messageText::setText, this::timelineUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        fadeOutHandler.removeMessages(MESSAGE_FADE_OUT);
    }

    @Override
    public void onResume() {
        super.onResume();

        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));
    }

    public void onTransitionCompleted() {
        // This is the best place to fire animations.
    }


    public void showSleepScore(int sleepScore) {
        int scoreColor = Styles.getSleepScoreColor(getActivity(), sleepScore);
        scoreGraph.setFillColor(scoreColor);
        scoreText.setTextColor(scoreColor);
        ValueAnimator updateAnimation = scoreGraph.animationForNewValue(sleepScore, Animations.Properties.createWithDelay(250));
        if (updateAnimation != null) {
            updateAnimation.addUpdateListener(a -> {
                String score = a.getAnimatedValue().toString();
                scoreText.setText(score);
            });

            updateAnimation.start();
        }
    }

    public void showInsights(@NonNull List<PreSleepInsight> preSleepInsights) {
        if (preSleepInsights.isEmpty()) {
            beforeSleepHeader.setVisibility(View.GONE);
        } else {
            beforeSleepItemContainer.removeViews(0, beforeSleepItemContainer.getChildCount());

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            int dividerWidth = (int) (0.14f * beforeSleepHeader.getMeasuredWidth()); // from comp
            View.OnClickListener onClick = new SafeOnClickListener(this::showInsight);
            for (int i = 0, size = preSleepInsights.size(); i < size; i++) {
                PreSleepInsight preSleepInsight = preSleepInsights.get(i);

                ImageView insightImage = (ImageView) inflater.inflate(R.layout.item_before_sleep, beforeSleepItemContainer, false);
                insightImage.setImageDrawable(preSleepInsight.getIcon(getActivity()));
                insightImage.setTag(i);
                insightImage.setTag(R.id.fragment_timeline_before_sleep_item_tag_insight, preSleepInsight);
                insightImage.setOnClickListener(onClick);
                beforeSleepItemContainer.addView(insightImage);

                if (i != size - 1) {
                    beforeSleepItemContainer.addView(Styles.createHorizontalDivider(getActivity(), dividerWidth));
                }
            }

            beforeSleepHeader.setVisibility(View.VISIBLE);
            beforeSleepHeader.forceLayout();
        }
    }

    public void bindTimeline(@Nullable Timeline timeline) {
        if (timeline != null) {
            showSleepScore(timeline.getScore());

            if (timeline.getPreSleepInsights() != null && !timeline.getPreSleepInsights().isEmpty()) {
                showInsights(timeline.getPreSleepInsights());
            }


            if (timeline.getSegments().isEmpty()) {
                timelineEventsHeader.setVisibility(View.INVISIBLE);
            } else {
                timelineEventsHeader.setVisibility(View.VISIBLE);

                bindAndSubscribe(Views.observeNextLayout(listView), ignored -> {
                    this.scrollContentAperture = segmentAdapter.getTotalItemHeight() - listView.getMeasuredHeight();
                    this.timeScrubberTrackHeight = listView.getMeasuredHeight() - timeScrubber.getMeasuredHeight();

                    updateTimeScrubber();
                    timeScrubber.forceLayout(); // Does not happen implicitly
                }, Functions.LOG_ERROR);
            }
        } else {
            scoreGraph.setTrackColor(getResources().getColor(R.color.border));

            showInsights(Collections.emptyList());
            timelineEventsHeader.setVisibility(View.INVISIBLE);

            timeScrubber.setVisibility(View.INVISIBLE);
        }
    }

    public void timelineUnavailable(@Nullable Throwable e) {
        scoreGraph.setTrackColor(getResources().getColor(R.color.border));
        scoreGraph.setValue(0);
        scoreText.setText(R.string.missing_data_placeholder);
        scoreText.setTextColor(getResources().getColor(R.color.text_dark));

        if (e != null) {
            messageText.setText(getString(R.string.timeline_error_message, e.getMessage()));
        } else {
            messageText.setText(R.string.missing_data_placeholder);
        }

        timeScrubber.setVisibility(View.INVISIBLE);
    }


    public DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }


    public void showInsight(@NonNull View sender) {
        View view = beforeSleepItemContainer.findViewWithTag(selectedBeforeSleepInsight);
        if (view == sender) {
            beforeSleepMessage.setText(null);
            beforeSleepMessage.setVisibility(View.GONE);
        } else {
            PreSleepInsight insight = (PreSleepInsight) sender.getTag(R.id.fragment_timeline_before_sleep_item_tag_insight);
            beforeSleepMessage.setText(insight.getMessage());
            bindAndSubscribe(markdown.render(insight.getMessage()), beforeSleepMessage::setText, Functions.LOG_ERROR);
            beforeSleepMessage.setVisibility(View.VISIBLE);

            this.selectedBeforeSleepInsight = (int) sender.getTag();
        }
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

    //region Time Scrubber

    private void snapInTimeScrubber() {
        PropertyAnimatorProxy.stop(timeScrubber);
        fadeOutHandler.removeMessages(MESSAGE_FADE_OUT);

        timeScrubber.setAlpha(1f);
        timeScrubber.setVisibility(View.VISIBLE);
    }

    private void fadeInTimeScrubber() {
        fadeOutHandler.removeMessages(MESSAGE_FADE_OUT);

        if (timeScrubber.getVisibility() == View.INVISIBLE) {
            animate(timeScrubber)
                    .setDuration(Animations.DURATION_MINIMUM)
                    .fadeIn()
                    .start();
        }
    }

    private void fadeOutTimeScrubber() {
        fadeOutHandler.removeMessages(MESSAGE_FADE_OUT);

        if (timeScrubber.getAlpha() == 1f) {
            animate(timeScrubber)
                    .setDuration(Animations.DURATION_MINIMUM)
                    .fadeOut(View.INVISIBLE)
                    .start();
        }
    }

    private void scheduleFadeOutTimeScrubber() {
        fadeOutHandler.removeMessages(MESSAGE_FADE_OUT);
        fadeOutHandler.sendEmptyMessageDelayed(MESSAGE_FADE_OUT, MESSAGE_FADE_OUT_DELAY);
    }

    private void updateTimeScrubber() {
        // OnScrollListener will fire immediately after rotation before
        // the list view has laid itself out, have to guard against that.
        if (listView.getChildCount() == 0) {
            return;
        }

        int firstVisiblePosition = listView.getFirstVisiblePosition();
        int firstVisibleSegment = ListViews.getAdapterPosition(listView, firstVisiblePosition);

        float headerInset;
        float scrollTop;
        if (firstVisiblePosition < listView.getHeaderViewsCount()) {
            headerInset = timelineEventsHeader.getBottom();
            scrollTop = 0f;
        } else {
            headerInset = 0f;

            View topView = listView.getChildAt(0);
            float scaleFactor = -topView.getTop() / (float) topView.getMeasuredHeight();
            scrollTop = segmentAdapter.getHeightOfItems(0, firstVisibleSegment, scaleFactor);
        }

        float multiple = Math.min(1f, scrollTop / scrollContentAperture);
        float timestampY = headerInset + (timeScrubberTrackHeight * multiple);

        int itemPosition = ListViews.getPositionForY(listView, timestampY);
        TimelineSegment segment = segmentAdapter.getItem(itemPosition);
        timeScrubber.setY(timestampY);
        timeScrubber.setDateTime(segment.getTimestamp());
    }


    private class TimelineScrollListener extends ListViews.TouchAndScrollListener {
        @Override
        protected void onScrollStateChanged(@NonNull AbsListView absListView, int oldState, int newState) {
            if (segmentAdapter.getCount() > 0) {
                if (newState == SCROLL_STATE_FLING) {
                    snapInTimeScrubber();
                } else if (newState == SCROLL_STATE_IDLE && oldState == SCROLL_STATE_FLING) {
                    scheduleFadeOutTimeScrubber();
                }
            }
        }

        @Override
        public void onScroll(AbsListView listView, int firstVisiblePosition, int visibleItemCount, int totalItemCount) {
            if (segmentAdapter.getCount() > 0) {
                updateTimeScrubber();
            }

            if (firstVisiblePosition == 0) {
                pullSmartAlarmOnScreen();
            } else {
                pushSmartAlarmOffScreen();
            }
        }

        @Override
        protected void onTouchDown(@NonNull AbsListView absListView) {
            fadeInTimeScrubber();
        }

        @Override
        protected void onTouchUp(@NonNull AbsListView absListView) {
            scheduleFadeOutTimeScrubber();
        }
    }

    //endregion
}
