package is.hello.sense.ui.fragments;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import is.hello.sense.ui.adapter.HeaderFooterRecyclerAdapter;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.ui.common.ViewUtil;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.widget.PieGraphView;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.TimestampTextView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, TimelineSegmentAdapter.OnItemClickedListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";


    @Inject DateFormatter dateFormatter;
    @Inject TimelinePresenter timelinePresenter;

    private static RecyclerView.RecycledViewPool SEGMENT_POOL = new RecyclerView.RecycledViewPool();
    static {
        SEGMENT_POOL.setMaxRecycledViews(TimelineSegmentAdapter.VIEW_ITEM_TYPE, 15);
    }

    private RecyclerView recyclerView;
    private LinearLayoutManager timelineLayoutManager;
    private HeaderFooterRecyclerAdapter headerFooterAdapter;
    private TimelineSegmentAdapter segmentAdapter;

    private TimestampTextView timeScrubber;
    private float timeScrubberMargin;

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

        this.segmentAdapter = new TimelineSegmentAdapter(getActivity(), this);
        this.timeScrubberMargin = getResources().getDimensionPixelSize(R.dimen.gap_medium);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.timeScrubber = (TimestampTextView) view.findViewById(R.id.fragment_timeline_scrubber);

        this.recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        recyclerView.setOnScrollListener(new TimelineScrollListener());
        recyclerView.setRecycledViewPool(SEGMENT_POOL);

        this.timelineLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(timelineLayoutManager);


        this.headerFooterAdapter = new HeaderFooterRecyclerAdapter(segmentAdapter);
        recyclerView.setAdapter(headerFooterAdapter);

        this.headerView = inflater.inflate(R.layout.sub_fragment_timeline_header, recyclerView, false);

        this.scoreGraph = (PieGraphView) headerView.findViewById(R.id.fragment_timeline_sleep_score_chart);
        this.scoreText = (TextView) headerView.findViewById(R.id.fragment_timeline_sleep_score);
        this.messageTextLabel = (TextView) headerView.findViewById(R.id.fragment_timeline_message_label);
        this.messageText = (TextView) headerView.findViewById(R.id.fragment_timeline_message);

        this.dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));

        headerFooterAdapter.addHeaderView(headerView);


        this.timelineEventsHeader = (TextView) inflater.inflate(R.layout.item_section_header, recyclerView, false);
        timelineEventsHeader.setText(R.string.title_events_timeline);
        timelineEventsHeader.setVisibility(View.INVISIBLE);
        headerFooterAdapter.addHeaderView(timelineEventsHeader);

        View listFooter = new View(getActivity());
        listFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_outer));
        headerFooterAdapter.addFooterView(listFooter);


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

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        SEGMENT_POOL.clear();
    }

    public void showSleepScore(int sleepScore) {
        if (sleepScore > 0) {
            scoreGraph.setTrackColor(Color.TRANSPARENT);
        } else {
            scoreGraph.setTrackColor(getResources().getColor(R.color.border));
        }

        scoreGraph.setFillColor(getResources().getColor(Styles.getSleepScoreColorRes(sleepScore)));
        ValueAnimator updateAnimation = scoreGraph.animationForNewValue(sleepScore, Animation.Properties.createWithDelay(250));
        if (updateAnimation != null) {
            updateAnimation.addUpdateListener(a -> {
                String score = a.getAnimatedValue().toString();
                scoreText.setText(score);
            });

            updateAnimation.start();
        }
    }

    public void showInsights(@NonNull List<PreSleepInsight> preSleepInsights) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        if (insightsContainer != null) {
            int childCount = insightsContainer.getChildCount();
            if (childCount > 2) {
                insightsContainer.removeViews(2, childCount - 2);
            }
        } else {
            this.insightsContainer = (LinearLayout) inflater.inflate(R.layout.sub_fragment_before_sleep, recyclerView, false);
            headerFooterAdapter.addFooterView(insightsContainer);
        }

        for (PreSleepInsight preSleepInsight : preSleepInsights) {
            TextView insightText = (TextView) inflater.inflate(R.layout.item_before_sleep, insightsContainer, false);
            insightText.setCompoundDrawablesRelativeWithIntrinsicBounds(preSleepInsight.getIconResource(), 0, 0, 0);
            insightText.setText(preSleepInsight.getMessage());
            insightsContainer.addView(insightText);
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

                bindAndSubscribe(ViewUtil.onGlobalLayout(recyclerView).take(1), view -> {
                    updateTimeScrubber();
                    animate(timeScrubber)
                            .fadeIn()
                            .start();
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


    @Override
    public void onItemClicked(int position) {
        TimelineSegment segment = segmentAdapter.getItem(position);
        if (segment.getEventType() != null) {
            TimelineEventDialogFragment dialogFragment = TimelineEventDialogFragment.newInstance(segment);
            dialogFragment.show(getFragmentManager(), TimelineEventDialogFragment.TAG);
        }

        Analytics.event(Analytics.EVENT_TIMELINE_ACTION, Analytics.createProperties(Analytics.PROP_TIMELINE_ACTION, Analytics.PROP_TIMELINE_ACTION_TAP_EVENT));
    }


    public DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }


    @Override
    public void onUserWillPullDownTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_open);
        shareButton.setImageResource(R.drawable.icon_share_disabled);
    }

    @Override
    public void onUserDidPushUpTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_closed);
        shareButton.setImageResource(R.drawable.icon_share_enabled);
    }


    private void updateTimeScrubber() {
        int firstVisiblePosition = timelineLayoutManager.findFirstVisibleItemPosition();
        if (firstVisiblePosition == 0) {
            timeScrubber.setY(timeScrubberMargin + headerView.getBottom());
        } else {
            timeScrubber.setY(timeScrubberMargin);
        }

        int segmentPosition = headerFooterAdapter.getPositionInWrapper(firstVisiblePosition);
        TimelineSegment segment = segmentAdapter.getItem(segmentPosition);
        timeScrubber.setDateTime(segment.getTimestamp());
    }

    private class TimelineScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (segmentAdapter.getItemCount() > 0) {
                int firstVisiblePosition = timelineLayoutManager.findFirstVisibleItemPosition();
                int segmentPosition = headerFooterAdapter.getPositionInWrapper(firstVisiblePosition);
                TimelineSegment segment = segmentAdapter.getItem(segmentPosition);
                timeScrubber.setDateTime(segment.getTimestamp());

                updateTimeScrubber();
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (newState == RecyclerView.SCROLL_STATE_IDLE && segmentAdapter.getItemCount() > 0) {
                updateTimeScrubber();
            }
        }
    }
}
