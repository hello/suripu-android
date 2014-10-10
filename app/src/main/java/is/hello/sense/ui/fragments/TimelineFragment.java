package is.hello.sense.ui.fragments;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.TimelineSegmentDetailsDialogFragment;
import is.hello.sense.ui.widget.PieGraphView;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class TimelineFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    private PieGraphView scoreGraph;
    private TextView scoreText;
    private TextView messageText;

    @Inject DateFormatter dateFormatter;

    private TimelineSegmentAdapter segmentAdapter;
    private TimelinePresenter timelinePresenter;


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

        this.timelinePresenter = new TimelinePresenter(getDateTime());
        addPresenter(timelinePresenter);

        this.segmentAdapter = new TimelineSegmentAdapter(getActivity());

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(segmentAdapter);
        listView.setOnItemClickListener(this);
        listView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);


        View headerView = inflater.inflate(R.layout.sub_fragment_timeline_header, listView, false);

        this.scoreGraph = (PieGraphView) headerView.findViewById(R.id.fragment_timeline_sleep_score_chart);
        this.scoreText = (TextView) headerView.findViewById(R.id.fragment_timeline_sleep_score);
        this.messageText = (TextView) headerView.findViewById(R.id.fragment_timeline_message);
        scoreGraph.setOnClickListener(this::showDebug);

        TextView dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(getDateTime()));

        listView.addHeaderView(headerView, null, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Timeline> boundMainTimeline = bindFragment(this, timelinePresenter.mainTimeline);
        track(boundMainTimeline.subscribe(this::bindSummary, this::presentError));
        track(boundMainTimeline.map(Timeline::getSegments)
                               .subscribe(segmentAdapter::bindSegments, segmentAdapter::handleError));

        Observable<CharSequence> renderedMessage = bindFragment(this, timelinePresenter.renderedTimelineMessage);
        track(renderedMessage.subscribe(messageText::setText, error -> messageText.setText(R.string.missing_data_placeholder)));
    }

    public void onTransitionCompleted() {
        // This is the best place to fire animations.
    }


    public void bindSummary(@NonNull Timeline timeline) {
        int sleepScore = timeline.getScore();
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

    public void presentError(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        TimelineSegment segment = (TimelineSegment) adapterView.getItemAtPosition(position);
        if (segment.getEventType() != null) {
            TimelineSegmentDetailsDialogFragment dialogFragment = TimelineSegmentDetailsDialogFragment.newInstance(segment);
            dialogFragment.show(getFragmentManager(), TimelineSegmentDetailsDialogFragment.TAG);
        }
    }

    public void showDebug(@NonNull View sender) {
        startActivity(new Intent(getActivity(), DebugActivity.class));
    }


    public DateTime getDateTime() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }
}
