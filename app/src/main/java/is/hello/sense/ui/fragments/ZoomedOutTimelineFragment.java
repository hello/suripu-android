package is.hello.sense.ui.fragments;

import android.graphics.PointF;
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
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenter;
import is.hello.sense.ui.adapter.ZoomedOutTimelineAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ZoomedOutTimelineLayoutManager;
import is.hello.sense.ui.widget.timeline.ZoomedOutTimelineDecoration;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.DateFormatter;

public class ZoomedOutTimelineFragment extends InjectionFragment implements ZoomedOutTimelineAdapter.OnItemClickedListener {
    public static final String TAG = ZoomedOutTimelineFragment.class.getSimpleName();

    private static final String ARG_START_DATE = ZoomedOutTimelineFragment.class.getName() + ".ARG_START_DATE";
    private static final String ARG_FIRST_TIMELINE = ZoomedOutTimelineFragment.class.getName() + ".ARG_FIRST_TIMELINE";

    @Inject ZoomedOutTimelinePresenter presenter;
    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;

    private TextView monthText;
    private RecyclerView recyclerView;
    private ZoomedOutTimelineLayoutManager layoutManager;
    private LocalDate startDate;

    public static ZoomedOutTimelineFragment newInstance(@NonNull LocalDate startTime, @Nullable Timeline firstTimeline) {
        ZoomedOutTimelineFragment fragment = new ZoomedOutTimelineFragment();

        Bundle arguments = new Bundle();
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
            Timeline firstTimeline = (Timeline) getArguments().getSerializable(ARG_FIRST_TIMELINE);
            presenter.cacheTimeline(startDate, firstTimeline);
        }

        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_zoomed_out_timeline, container, false);
        view.setClickable(true);

        this.monthText = (TextView) view.findViewById(R.id.fragment_zoomed_out_timeline_month);
        monthText.setText(dateFormatter.formatAsTimelineNavigatorDate(startDate));
        Views.setSafeOnClickListener(monthText, ignored -> getFragmentManager().popBackStack());

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_zoomed_out_timeline_recycler_view);
        recyclerView.addItemDecoration(new ZoomedOutTimelineDecoration(getResources()));
        recyclerView.addOnScrollListener(new SnappingScrollListener());

        this.layoutManager = new ZoomedOutTimelineLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        ZoomedOutTimelineAdapter adapter = new ZoomedOutTimelineAdapter(getActivity(),
                                                                        presenter,
                                                                        preferences.getAccountCreationDate());
        adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);

        Button todayButton = (Button) view.findViewById(R.id.fragment_zoomed_out_timeline_today);
        todayButton.setOnClickListener(this::jumpToToday);

        int position = presenter.getDatePosition(startDate);
        if (position == 0) {
            // The second item from the right at the beginning of the
            // navigator is a special case. Because the item is already
            // visible without scrolling, the layout manager will try to
            // take a shortcut and not scroll at all. So we have to give
            // a specific offset if we want it to be centered. Of course,
            // we can only get the offset after layout.

            layoutManager.postLayout(() -> {
                recyclerView.scrollBy(-layoutManager.getItemWidth(), 0);
                recyclerView.post(presenter::retrieveTimelines);
            });
        } else {
            layoutManager.scrollToPosition(position);
            recyclerView.post(presenter::retrieveTimelines);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        recyclerView.clearOnScrollListeners();
    }

    public void jumpToToday(@NonNull View sender) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int calculateTimeForScrolling(int dx) {
                return super.calculateTimeForScrolling(dx) / 4;
            }

            @Override
            protected int calculateTimeForDeceleration(int dx) {
                return super.calculateTimeForDeceleration(dx) / 4;
            }
        };
        smoothScroller.setTargetPosition(0);
        layoutManager.startSmoothScroll(smoothScroller);
    }

    @Override
    public void onItemClicked(@NonNull View itemView, int position) {
        // Tried implementing this using the first/last visible items
        // from the linear layout manager, the children of the recycler
        // view, and neither worked 100% of the time. This does.

        // Guard against rapid taps
        if (!isVisible()) {
            return;
        }

        if (itemView.getAlpha() < 1f) {
            int recyclerViewCenter = recyclerView.getMeasuredWidth() / 2;
            if (itemView.getRight() < recyclerViewCenter) {
                recyclerView.smoothScrollBy(-layoutManager.getItemWidth(), 0);
            } else {
                recyclerView.smoothScrollBy(layoutManager.getItemWidth(), 0);
            }
        } else {
            LocalDate newDate = presenter.getDateAt(position);
            Timeline timeline = presenter.getCachedTimeline(newDate);
            ((OnTimelineDateSelectedListener) getActivity()).onTimelineSelected(newDate, timeline);
        }
    }


    class SnappingScrollListener extends RecyclerView.OnScrollListener {
        int previousState = RecyclerView.SCROLL_STATE_IDLE;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            View centerChild = recyclerView.findChildViewUnder(recyclerView.getWidth() / 2, 0);
            ZoomedOutTimelineAdapter.ViewHolder holder = (ZoomedOutTimelineAdapter.ViewHolder) recyclerView.getChildViewHolder(centerChild);
            monthText.setText(dateFormatter.formatAsTimelineNavigatorDate(holder.getDate()));
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (previousState != RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToNearestItem(recyclerView);
                presenter.retrieveTimelines();
            }

            this.previousState = newState;
        }

        public void snapToNearestItem(RecyclerView recyclerView) {
            int containerMidX = recyclerView.getWidth() / 2;
            View centerView = recyclerView.findChildViewUnder(containerMidX, 0);
            int centerViewMidX = (centerView.getLeft() + centerView.getRight()) / 2;
            int distanceToNotch = centerViewMidX - containerMidX;
            if (distanceToNotch != 0) {
                recyclerView.smoothScrollBy(distanceToNotch, 0);
            }
        }
    }


    public interface OnTimelineDateSelectedListener {
        void onTimelineSelected(@NonNull LocalDate date, @Nullable Timeline timeline);
    }
}
