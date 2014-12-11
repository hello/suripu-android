package is.hello.sense.ui.fragments;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import is.hello.sense.api.model.PreSleepInsight;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ListViewUtil;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.ui.common.Views;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.widget.PieGraphView;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.TimestampTextView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, AdapterView.OnItemClickListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";


    @Inject DateFormatter dateFormatter;
    @Inject TimelinePresenter timelinePresenter;

    private ListView listView;
    private TimelineSegmentAdapter segmentAdapter;

    private TimestampTextView timeScrubber;
    private int timeScrubberTopMargin = 0;
    private boolean headerTallerThanList = false;
    private int totalHeaderHeight = 0;
    private int listViewContentHeight = 0;

    private View headerView;
    private ImageButton menuButton;
    private ImageButton shareButton;
    private TextView dateText;
    private PieGraphView scoreGraph;
    private TextView scoreText;
    private TextView messageTextLabel;
    private TextView messageText;

    private TextView timelineEventsHeader;
    private LinearLayout insightsContainer;


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
        this.timeScrubberTopMargin = getResources().getDimensionPixelSize(R.dimen.gap_medium);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.timeScrubber = (TimestampTextView) view.findViewById(R.id.fragment_timeline_scrubber);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        listView.setOnScrollListener(new TimelineScrollListener());
        listView.setOnItemClickListener(this);

        this.headerView = inflater.inflate(R.layout.sub_fragment_timeline_header, listView, false);

        this.scoreGraph = (PieGraphView) headerView.findViewById(R.id.fragment_timeline_sleep_score_chart);
        this.scoreText = (TextView) headerView.findViewById(R.id.fragment_timeline_sleep_score);
        this.messageTextLabel = (TextView) headerView.findViewById(R.id.fragment_timeline_message_label);
        this.messageText = (TextView) headerView.findViewById(R.id.fragment_timeline_message);

        this.dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));

        listView.addHeaderView(headerView, null, false);


        this.timelineEventsHeader = (TextView) inflater.inflate(R.layout.item_section_header, listView, false);
        timelineEventsHeader.setText(R.string.title_events_timeline);
        timelineEventsHeader.setVisibility(View.INVISIBLE);
        listView.addHeaderView(timelineEventsHeader, null, false);

        View spacingFooter = new View(getActivity());
        spacingFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_outer));
        listView.addFooterView(spacingFooter, null, false);


        this.menuButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_menu);
        menuButton.setOnClickListener(ignored -> {
            HomeActivity activity = (HomeActivity) getActivity();
            activity.getSlidingLayersView().toggle();
        });

        this.shareButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_share);
        shareButton.setOnClickListener(ignored -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "http://hello.is");
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
        });


        this.insightsContainer = (LinearLayout) inflater.inflate(R.layout.sub_fragment_before_sleep, listView, false);
        insightsContainer.setVisibility(View.GONE);
        listView.addFooterView(insightsContainer, null, false);

        // Always do this after adding headers and footer views,
        // we have to support Android versions under 4.4 KitKat.
        listView.setAdapter(segmentAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Timeline> boundMainTimeline = bind(timelinePresenter.mainTimeline);
        subscribe(boundMainTimeline, this::bindTimeline, this::timelineUnavailable);

        Observable<List<TimelineSegment>> segments = boundMainTimeline.map(timeline -> {
            if (timeline != null)
                return timeline.getSegments();
            else
                return Collections.emptyList();
        });
        subscribe(segments, segmentAdapter::bindSegments, segmentAdapter::handleError);

        Observable<CharSequence> renderedMessage = timelinePresenter.renderedTimelineMessage;
        bindAndSubscribe(renderedMessage, messageText::setText, this::timelineUnavailable);
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
        if (sleepScore > 0) {
            scoreGraph.setTrackColor(Color.TRANSPARENT);
        } else {
            scoreGraph.setTrackColor(getResources().getColor(R.color.border));
        }

        scoreGraph.setFillColor(getResources().getColor(Styles.getSleepScoreColorRes(sleepScore)));
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
            insightsContainer.setVisibility(View.GONE);
        } else {
            LayoutInflater inflater = LayoutInflater.from(getActivity());

            int childCount = insightsContainer.getChildCount();
            if (childCount > 2) {
                insightsContainer.removeViews(2, childCount - 2);
            }

            for (PreSleepInsight preSleepInsight : preSleepInsights) {
                TextView insightText = (TextView) inflater.inflate(R.layout.item_before_sleep, insightsContainer, false);
                insightText.setCompoundDrawablesRelativeWithIntrinsicBounds(preSleepInsight.getIconResource(), 0, 0, 0);
                insightText.setText(preSleepInsight.getMessage());
                insightsContainer.addView(insightText);
            }

            insightsContainer.setVisibility(View.VISIBLE);
            insightsContainer.forceLayout();
        }
    }

    public void bindTimeline(@Nullable Timeline timeline) {
        if (timeline != null) {
            showSleepScore(timeline.getScore());

            if (timeline.getPreSleepInsights() != null && !timeline.getPreSleepInsights().isEmpty()) {
                showInsights(timeline.getPreSleepInsights());
            }


            if (timeline.getSegments().isEmpty()) {
                messageTextLabel.setVisibility(View.INVISIBLE);
                messageText.setGravity(Gravity.CENTER);
                timelineEventsHeader.setVisibility(View.INVISIBLE);
            } else {
                timelineEventsHeader.setVisibility(View.VISIBLE);
                messageTextLabel.setVisibility(View.VISIBLE);
                messageText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                bindAndSubscribe(Views.observeNextLayout(listView), ignored -> {
                    this.totalHeaderHeight = headerView.getMeasuredHeight() + timelineEventsHeader.getMeasuredHeight();
                    this.headerTallerThanList = headerView.getMeasuredHeight() > listView.getMeasuredHeight();
                    if (headerTallerThanList) {
                        this.listViewContentHeight = (listView.getMeasuredHeight() - timelineEventsHeader.getMeasuredHeight());
                    } else {
                        this.listViewContentHeight = (listView.getMeasuredHeight() - totalHeaderHeight);
                    }

                    updateTimeScrubber();
                    timeScrubber.forceLayout(); // Does not happen implicitly
                    animate(timeScrubber).fadeIn().startAfterLayout();
                }, Functions.LOG_ERROR);
            }
        } else {
            scoreGraph.setTrackColor(getResources().getColor(R.color.border));

            messageTextLabel.setVisibility(View.INVISIBLE);
            messageText.setGravity(Gravity.CENTER);

            showInsights(Collections.emptyList());
            timelineEventsHeader.setVisibility(View.INVISIBLE);

            timeScrubber.setVisibility(View.INVISIBLE);
        }
    }

    public void timelineUnavailable(@Nullable Throwable e) {
        scoreGraph.setTrackColor(getResources().getColor(R.color.border));
        scoreGraph.setValue(0);
        scoreText.setText(R.string.missing_data_placeholder);

        messageTextLabel.setVisibility(View.INVISIBLE);
        messageText.setGravity(Gravity.CENTER);

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

        Analytics.event(Analytics.EVENT_TIMELINE_ACTION, Analytics.createProperties(Analytics.PROP_TIMELINE_ACTION, Analytics.PROP_TIMELINE_ACTION_TAP_EVENT));
    }

    private void updateTimeScrubber() {
        View topView = listView.getChildAt(0);

        // OnScrollListener will fire immediately after rotation before
        // the list view has laid itself out, have to guard against that.
        if (topView == null) {
            return;
        }

        int firstVisiblePosition = listView.getFirstVisiblePosition();
        int firstVisibleSegment = ListViewUtil.getAdapterPosition(listView, firstVisiblePosition);

        float scrolledAmount;
        if (firstVisiblePosition == 0) {
            scrolledAmount = headerTallerThanList ? 0 : -topView.getTop();
        } else if (firstVisiblePosition == 1) {
            scrolledAmount = headerTallerThanList ? 0 : (headerView.getMeasuredHeight() + -topView.getTop());
        } else {
            float scaleFactor = -topView.getTop() / (float) topView.getMeasuredHeight();
            float itemsHeight = segmentAdapter.getHeightOfItems(0, firstVisibleSegment, scaleFactor);
            scrolledAmount = headerTallerThanList ? itemsHeight : (totalHeaderHeight + itemsHeight);
        }

        float multiple = (scrolledAmount / segmentAdapter.getTotalItemHeight());
        float headerInset = headerTallerThanList ? headerView.getBottom() : headerView.getMeasuredHeight();
        float timestampY = (timeScrubberTopMargin + headerInset) + (listViewContentHeight * multiple);

        if (insightsContainer.getParent() != null) {
            int insightsVisibleHeight = listView.getMeasuredHeight() - insightsContainer.getTop();
            int amountVisible = insightsVisibleHeight / insightsContainer.getMeasuredHeight();
            float extraMarginFraction = timeScrubberTopMargin * amountVisible;
            timestampY -= insightsVisibleHeight + extraMarginFraction;
        }


        int itemPosition = ListViewUtil.getPositionForY(listView, timestampY);
        TimelineSegment segment = segmentAdapter.getItem(itemPosition);
        timeScrubber.setY(timestampY);
        timeScrubber.setDateTime(segment.getTimestamp());
    }


    private class TimelineScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView listView, int newState) {
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && segmentAdapter.getCount() > 0) {
                updateTimeScrubber();
            }
        }

        @Override
        public void onScroll(AbsListView listView, int firstVisiblePosition, int visibleItemCount, int totalItemCount) {
            if (segmentAdapter.getCount() > 0) {
                updateTimeScrubber();
            }
        }
    }
}
