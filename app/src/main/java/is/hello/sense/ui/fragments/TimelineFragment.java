package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineDate;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class TimelineFragment extends InjectionFragment {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";

    private TextView dateText;
    private TextView scoreText;
    private TextView messageText;

    @Inject ApiService apiService;

    private TimelinePresenter presenter;

    public static TimelineFragment newInstance() {
        return TimelineFragment.newInstance(TimelineDate.today());
    }

    public static TimelineFragment newInstance(@NonNull TimelineDate date) {
        TimelineFragment fragment = new TimelineFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimelineDate timelineDate = (TimelineDate) getArguments().getSerializable(ARG_DATE);
        this.presenter = new TimelinePresenter(apiService, timelineDate);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.dateText = (TextView) view.findViewById(R.id.fragment_timeline_date);
        this.scoreText = (TextView) view.findViewById(R.id.fragment_timeline_sleep_score);
        this.messageText = (TextView) view.findViewById(R.id.fragment_timeline_message);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<List<Timeline>> observable = bindFragment(this, presenter.timeline);
        observable.subscribe(this::bindData, this::presentError);
    }

    public void bindData(@NonNull List<Timeline> data) {
        Timeline now = data.get(0);
        dateText.setText(now.getDate());
        scoreText.setText(Long.toString(now.getScore()));
        messageText.setText(now.getMessage());
    }

    public void presentError(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
