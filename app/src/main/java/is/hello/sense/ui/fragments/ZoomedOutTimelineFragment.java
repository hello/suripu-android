package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.ZoomedOutTimelineInteractor;
import is.hello.sense.ui.adapter.ZoomedOutTimelineAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ZoomedOutTimelineLayoutManager;
import is.hello.sense.ui.widget.timeline.ZoomedOutTimelineDecoration;
import is.hello.sense.util.DateFormatter;

public class ZoomedOutTimelineFragment extends InjectionFragment
        implements ZoomedOutTimelineAdapter.OnItemClickedListener {
    public static final String TAG = ZoomedOutTimelineFragment.class.getSimpleName();

    private static final String ARG_START_DATE = ZoomedOutTimelineFragment.class.getName() + ".ARG_START_DATE";
    private static final String ARG_FIRST_TIMELINE = ZoomedOutTimelineFragment.class.getName() + ".ARG_FIRST_TIMELINE";

    @Inject
    ZoomedOutTimelineInteractor presenter;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;

    @Nullable
    private TextView monthText;
    @Nullable
    private RecyclerView recyclerView;
    @Nullable
    private ZoomedOutTimelineLayoutManager layoutManager;
    private ZoomedOutTimelineAdapter adapter;
    private LocalDate startDate;

    public static ZoomedOutTimelineFragment newInstance(@NonNull LocalDate startTime, @Nullable Timeline firstTimeline) {
        final ZoomedOutTimelineFragment fragment = new ZoomedOutTimelineFragment();

        final Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_START_DATE, startTime);
        arguments.putSerializable(ARG_FIRST_TIMELINE, firstTimeline);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.startDate = (LocalDate) getArguments().getSerializable(ARG_START_DATE);
        presenter.setFirstDate(DateFormatter.lastNight());

        if (getArguments().containsKey(ARG_FIRST_TIMELINE)) {
            final Timeline firstTimeline =
                    (Timeline) getArguments().getSerializable(ARG_FIRST_TIMELINE);
            presenter.cacheTimeline(startDate, firstTimeline);
        }

        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_zoomed_out_timeline, container, false);
        view.setClickable(true);

        this.monthText = (TextView) view.findViewById(R.id.fragment_zoomed_out_timeline_month);
        monthText.setText(dateFormatter.formatAsTimelineNavigatorDate(startDate));

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_zoomed_out_timeline_recycler_view);
        recyclerView.addItemDecoration(new ZoomedOutTimelineDecoration(getResources()));
        recyclerView.addOnScrollListener(new SnappingScrollListener());

        this.layoutManager = new ZoomedOutTimelineLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        this.adapter =
                new ZoomedOutTimelineAdapter(presenter,
                                             preferences.getAccountCreationDate());
        this.adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);

        final Button todayButton = (Button) view.findViewById(R.id.fragment_zoomed_out_timeline_today);
        todayButton.setOnClickListener(this::jumpToToday);

        final int position = presenter.getDatePosition(startDate);
        if (position == 0) {
            // The second item from the right at the beginning of the
            // navigator is a special case. Because the item is already
            // visible without scrolling, the layout manager will try to
            // take a shortcut and not scroll at all. So we have to give
            // a specific offset if we want it to be centered. Of course,
            // we can only get the offset after layout.

            layoutManager.postLayout(() -> recyclerView.post( () -> {
                recyclerView.scrollBy(-layoutManager.getItemWidth(), 0);
                presenter.retrieveTimelines();
            }));
        } else {
            layoutManager.scrollToPosition(position);
            recyclerView.post(presenter::retrieveTimelines);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        adapter.setOnItemClickedListener(null);
        if (layoutManager != null) {
            layoutManager.postLayout(null);
            layoutManager = null;
        }
        if (recyclerView != null) {
            recyclerView.clearOnScrollListeners();
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        monthText = null;
    }

    public void jumpToToday(@NonNull final View sender) {
        final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
            @Override
            protected int calculateTimeForScrolling(final int dx) {
                return super.calculateTimeForScrolling(dx) / 4;
            }

            @Override
            protected int calculateTimeForDeceleration(final int dx) {
                return super.calculateTimeForDeceleration(dx) / 4;
            }
        };
        smoothScroller.setTargetPosition(0);
        if (layoutManager != null) {
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }

    @Override
    public void onItemClicked(@NonNull final View itemView,
                              final int position) {
        // Tried implementing this using the first/last visible items
        // from the linear layout manager, the children of the recycler
        // view, and neither worked 100% of the time. This does.

        // Guard against rapid taps
        if (!isVisible()
                || recyclerView == null
                || layoutManager == null) {
            return;
        }

        if (itemView.getAlpha() < 0.95f) { // Can't use `< 1f` because of uneven view metrics
            final int recyclerViewCenter = recyclerView.getMeasuredWidth() / 2;
            if (itemView.getRight() < recyclerViewCenter) {
                recyclerView.smoothScrollBy(-layoutManager.getItemWidth(), 0);
            } else {
                recyclerView.smoothScrollBy(layoutManager.getItemWidth(), 0);
            }
        } else {
            final LocalDate newDate = presenter.getDateAt(position);
            final Timeline timeline = presenter.getCachedTimeline(newDate);
            ((OnTimelineDateSelectedListener) getActivity()).onTimelineSelected(newDate, timeline);
        }
    }


    private class SnappingScrollListener extends RecyclerView.OnScrollListener {
        int previousState = RecyclerView.SCROLL_STATE_IDLE;

        @Override
        public void onScrolled(final RecyclerView recyclerView,
                               final int dx,
                               final int dy) {
            final View centerChild = recyclerView.findChildViewUnder(recyclerView.getWidth() / 2, 0);
            final ZoomedOutTimelineAdapter.ViewHolder holder =
                    (ZoomedOutTimelineAdapter.ViewHolder) recyclerView.getChildViewHolder(centerChild);

            if (monthText != null) {
                monthText.setText(dateFormatter.formatAsTimelineNavigatorDate(holder.getDate()));
            }
        }

        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView,
                                         final int newState) {
            if (previousState != RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToNearestItem(recyclerView);
                presenter.retrieveTimelines();
            }

            this.previousState = newState;
        }

        private void snapToNearestItem(@NonNull final RecyclerView recyclerView) {
            final int containerMidX = recyclerView.getWidth() / 2;
            final View centerView = recyclerView.findChildViewUnder(containerMidX, 0);
            final int centerViewMidX = (centerView.getLeft() + centerView.getRight()) / 2;
            final int distanceToNotch = centerViewMidX - containerMidX;
            if (distanceToNotch != 0) {
                recyclerView.smoothScrollBy(distanceToNotch, 0);
            }
        }
    }


    public interface OnTimelineDateSelectedListener {
        void onTimelineSelected(@NonNull LocalDate date, @Nullable Timeline timeline);
    }
}
