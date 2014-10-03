package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class TimelineFragment extends InjectionFragment {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    private PieGraphView scoreGraph;
    private TextView dateText;
    private TextView scoreText;
    private TextView messageText;

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

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Timeline> timeline = bindFragment(this, presenter.timeline).filter(l -> !l.isEmpty())
                                                                              .map(l -> l.get(0));
        timeline.subscribe(this::bindData, this::presentError);
        timeline.map(Timeline::getSegments)
                .subscribe(segmentAdapter::bindSegments, segmentAdapter::handleError);
    }

    public void bindData(@NonNull Timeline timeline) {
        dateText.setText(dateFormatter.formatAsTimelineDate(timeline.getDate()));
        scoreText.setText(Long.toString(timeline.getScore()));
        scoreGraph.showSleepScore(timeline.getScore());
        messageText.setText(timeline.getMessage());
    }

    public void presentError(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
