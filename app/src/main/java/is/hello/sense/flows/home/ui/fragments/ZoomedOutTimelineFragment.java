package is.hello.sense.flows.home.ui.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.ui.views.ZoomedOutTimelineView;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.ZoomedOutTimelineInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.adapter.ZoomedOutTimelineAdapter;
import is.hello.sense.util.DateFormatter;

public class ZoomedOutTimelineFragment extends PresenterFragment<ZoomedOutTimelineView>
        implements ZoomedOutTimelineView.Listener {

    public static final String TAG = ZoomedOutTimelineFragment.class.getSimpleName();
    private static final String ARG_START_DATE = ZoomedOutTimelineFragment.class.getName() + ".ARG_START_DATE";
    private static final String ARG_FIRST_TIMELINE = ZoomedOutTimelineFragment.class.getName() + ".ARG_FIRST_TIMELINE";

    @Inject
    ZoomedOutTimelineInteractor zoomedOutTimelineInteractor;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;

    private OnTimelineDateSelectedListener listener = null;

    public static ZoomedOutTimelineFragment newInstance(@NonNull final LocalDate startTime,
                                                        @Nullable final Timeline firstTimeline) {
        final ZoomedOutTimelineFragment fragment = new ZoomedOutTimelineFragment();
        final Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_START_DATE, startTime);
        arguments.putSerializable(ARG_FIRST_TIMELINE, firstTimeline);
        fragment.setArguments(arguments);
        return fragment;
    }

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            final ZoomedOutTimelineAdapter adapter = new ZoomedOutTimelineAdapter(this.zoomedOutTimelineInteractor,
                                                                                  this.preferences.getAccountCreationDate());
            this.presenterView = new ZoomedOutTimelineView(getActivity(),
                                                           adapter);
            this.presenterView.setListener(this);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(getActivity() instanceof OnTimelineDateSelectedListener)) {
            throw new IllegalStateException("Activity must implement OnTimelineDateSelectedListener");
        }
        this.listener = ((OnTimelineDateSelectedListener) getActivity());
        this.zoomedOutTimelineInteractor.setFirstDate(DateFormatter.lastNight());
        addInteractor(this.zoomedOutTimelineInteractor);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final LocalDate startDate = (LocalDate) getArguments().getSerializable(ARG_START_DATE);
        if (getArguments().containsKey(ARG_FIRST_TIMELINE)) {
            if (startDate != null) {
                final Timeline firstTimeline = (Timeline) getArguments().getSerializable(ARG_FIRST_TIMELINE);
                this.zoomedOutTimelineInteractor.cacheTimeline(startDate, firstTimeline);
            }
        }
        this.presenterView.setMonthText(this.dateFormatter.formatAsTimelineNavigatorDate(startDate));
    }

    @Override
    protected void onRelease() {
        if (this.presenterView != null) {
            this.presenterView.setListener(null);
        }
        this.listener = null;
        super.onRelease();
    }
    //endregion

    //region ZoomedOutTimelineView.Listener
    @Override
    public String getFormattedDate(@NonNull final LocalDate localDate) {
        return this.dateFormatter.formatAsTimelineNavigatorDate(localDate);
    }

    @Override
    public void retrieveTimelines() {
        this.zoomedOutTimelineInteractor.retrieveTimelines();
    }

    @Override
    public void onTimelineClicked(final int position) {
        if (listener == null) {
            return;
        }
        final LocalDate newDate = this.zoomedOutTimelineInteractor.getDateAt(position);
        final Timeline timeline = this.zoomedOutTimelineInteractor.getCachedTimeline(newDate);
        this.listener.onTimelineSelected(newDate, timeline);
    }
    //endregion

    public interface OnTimelineDateSelectedListener {
        void onTimelineSelected(@NonNull LocalDate date, @Nullable Timeline timeline);
    }
}
