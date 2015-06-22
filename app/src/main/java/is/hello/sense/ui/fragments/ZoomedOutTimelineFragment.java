package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelineNavigatorPresenter;
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

    @Inject TimelineNavigatorPresenter presenter;
    @Inject DateFormatter dateFormatter;

    private TextView monthText;
    private RecyclerView recyclerView;
    private ZoomedOutTimelineLayoutManager layoutManager;
    private DateTime startDate;

    public static ZoomedOutTimelineFragment newInstance(@NonNull DateTime startTime, @Nullable Timeline firstTimeline) {
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

        this.startDate = (DateTime) getArguments().getSerializable(ARG_START_DATE);
        presenter.setFirstDate(DateFormatter.lastNight());

        if (getArguments().containsKey(ARG_FIRST_TIMELINE)) {
            Timeline firstTimeline = (Timeline) getArguments().getSerializable(ARG_FIRST_TIMELINE);
            presenter.cacheSingleTimeline(startDate, firstTimeline);
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

        ZoomedOutTimelineAdapter adapter = new ZoomedOutTimelineAdapter(getActivity(), presenter);
        adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);

        Button todayButton = (Button) view.findViewById(R.id.fragment_zoomed_out_timeline_today);
        todayButton.setOnClickListener(this::jumpToToday);

        int position = presenter.getDateTimePosition(startDate);
        if (position == 0) {
            // The second item from the right at the beginning of the
            // navigator is a special case. Because the item is already
            // visible without scrolling, the layout manager will try to
            // take a shortcut and not scroll at all. So we have to give
            // a specific offset if we want it to be centered. Of course,
            // we can only get the offset after layout.

            layoutManager.postLayout(() -> layoutManager.scrollToPositionWithOffset(position, -layoutManager.getItemWidth()));
        } else {
            layoutManager.scrollToPosition(position);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        recyclerView.clearOnScrollListeners();
    }

    public void jumpToToday(@NonNull View sender) {
        recyclerView.smoothScrollToPosition(0);
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
            DateTime newDate = presenter.getDateTimeAt(position);
            Timeline timeline = presenter.retrieveCachedTimeline(newDate);
            ((OnTimelineDateSelectedListener) getActivity()).onTimelineSelected(newDate, timeline);
        }
    }


    class SnappingScrollListener extends RecyclerView.OnScrollListener {
        int previousState = RecyclerView.SCROLL_STATE_IDLE;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            ZoomedOutTimelineAdapter.ItemViewHolder holder = (ZoomedOutTimelineAdapter.ItemViewHolder) recyclerView.findViewHolderForLayoutPosition(layoutManager.findLastVisibleItemPosition());
            monthText.setText(dateFormatter.formatAsTimelineNavigatorDate(holder.date));
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (previousState == RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                presenter.suspend();
            } else if (previousState != RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToNearestItem(recyclerView);
                presenter.resume();
            }

            this.previousState = newState;
        }

        public void snapToNearestItem(RecyclerView recyclerView) {
            int lastItem = layoutManager.findLastVisibleItemPosition();
            int lastCompleteItem = layoutManager.findLastCompletelyVisibleItemPosition();
            if (lastItem != lastCompleteItem) {
                View itemView = recyclerView.findViewHolderForLayoutPosition(lastItem).itemView;
                int width = itemView.getMeasuredWidth();
                int x = Math.abs(itemView.getRight());

                recyclerView.stopScroll();
                if (x > width / 2) {
                    recyclerView.smoothScrollToPosition(lastItem);
                } else {
                    recyclerView.smoothScrollToPosition(layoutManager.findFirstVisibleItemPosition());
                }
            }
        }
    }


    public interface OnTimelineDateSelectedListener {
        void onTimelineSelected(@NonNull DateTime date, @Nullable Timeline timeline);
    }
}
