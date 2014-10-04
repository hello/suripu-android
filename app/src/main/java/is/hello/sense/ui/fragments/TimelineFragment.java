package is.hello.sense.ui.fragments;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.PieGraphView;
import is.hello.sense.util.ColorUtils;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class TimelineFragment extends InjectionFragment {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    private PieGraphView scoreGraph;
    private TextView dateText;
    private TextView scoreText;
    private TextView messageText;

    private TextView averageTemperature;
    private TextView averageHumidity;
    private TextView averageParticulates;

    @Inject ApiService apiService;
    @Inject DateFormatter dateFormatter;

    private TimelineSegmentAdapter segmentAdapter;
    private TimelinePresenter presenter;


    public static TimelineFragment newInstance() {
        return TimelineFragment.newInstance(new DateTime(2014, 9, 22, 12, 0));
    }

    public static TimelineFragment newInstance(@NonNull DateTime date) {
        TimelineFragment fragment = new TimelineFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DateTime timelineDate = (DateTime) getArguments().getSerializable(ARG_DATE);
        this.presenter = new TimelinePresenter(apiService, timelineDate);
        this.segmentAdapter = new TimelineSegmentAdapter(getActivity());

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(segmentAdapter);


        View headerView = inflater.inflate(R.layout.sub_fragment_timeline_header, listView, false);

        this.scoreGraph = (PieGraphView) headerView.findViewById(R.id.fragment_timeline_sleep_score_chart);
        this.dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        this.scoreText = (TextView) headerView.findViewById(R.id.fragment_timeline_sleep_score);
        this.messageText = (TextView) headerView.findViewById(R.id.fragment_timeline_message);

        listView.addHeaderView(headerView, null, false);


        View conditionsView = inflater.inflate(R.layout.sub_fragment_average_conditions, listView, false);

        this.averageTemperature = (TextView) conditionsView.findViewById(R.id.fragment_timeline_average_temp);
        this.averageHumidity = (TextView) conditionsView.findViewById(R.id.fragment_timeline_average_humidity);
        this.averageParticulates = (TextView) conditionsView.findViewById(R.id.fragment_timeline_average_dust);

        listView.addHeaderView(conditionsView, null, false);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!hasSubscriptions()) {
            Observable<Timeline> boundMainTimeline = bindFragment(this, presenter.mainTimeline);
            track(boundMainTimeline.subscribe(this::bindSummary, this::presentError));
            track(boundMainTimeline.map(Timeline::getSegments)
                                   .subscribe(segmentAdapter::bindSegments, segmentAdapter::handleError));

            Observable<CharSequence> renderedMessage = bindFragment(this, presenter.renderedTimelineMessage);
            track(renderedMessage.subscribe(messageText::setText, error -> {}));
        }
    }

    public void bindSummary(@NonNull Timeline timeline) {
        dateText.setText(dateFormatter.formatAsTimelineDate(timeline.getDate()));

        int sleepScore = timeline.getScore();
        scoreGraph.setFillColor(getResources().getColor(ColorUtils.colorResForSleepDepth(sleepScore)));
        ValueAnimator updateAnimation = scoreGraph.animateToNewValue(sleepScore);
        if (updateAnimation != null) {
            updateAnimation.addUpdateListener(a -> {
                String score = a.getAnimatedValue().toString();
                scoreText.setText(score);
            });
        }
    }

    public void presentError(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
