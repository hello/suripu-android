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
import is.hello.sense.ui.adapter.TimelineNavigatorAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.TimelineNavigatorLayoutManager;
import is.hello.sense.ui.widget.TimelineItemDecoration;
import is.hello.sense.util.DateFormatter;

public class TimelineNavigatorFragment extends InjectionFragment implements TimelineNavigatorAdapter.OnItemClickedListener {
    public static final String TAG = TimelineNavigatorFragment.class.getSimpleName();

    private static final String ARG_START_DATE = TimelineNavigatorFragment.class.getName() + ".ARG_START_DATE";
    private static final String ARG_FIRST_TIMELINE = TimelineNavigatorFragment.class.getName() + ".ARG_FIRST_TIMELINE";

    @Inject TimelineNavigatorPresenter presenter;

    private TextView monthText;
    private RecyclerView recyclerView;
    private TimelineNavigatorLayoutManager layoutManager;
    private DateTime startDate;

    public static TimelineNavigatorFragment newInstance(@NonNull DateTime startTime, @Nullable Timeline firstTimeline) {
        TimelineNavigatorFragment fragment = new TimelineNavigatorFragment();

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
        View view = inflater.inflate(R.layout.fragment_timeline_navigator, container, false);

        this.monthText = (TextView) view.findViewById(R.id.fragment_timeline_navigator_month);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_navigator_recycler_view);
        recyclerView.addItemDecoration(new TimelineItemDecoration(getResources(), R.drawable.graph_grid_fill_top_down, R.dimen.divider_size));

        TimelineScrollListener timelineScrollListener = new TimelineScrollListener();
        recyclerView.setOnScrollListener(timelineScrollListener);

        this.layoutManager = new TimelineNavigatorLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        TimelineNavigatorAdapter adapter = new TimelineNavigatorAdapter(getActivity(), presenter);
        adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);

        Button todayButton = (Button) view.findViewById(R.id.fragment_timeline_navigator_today);
        todayButton.setOnClickListener(this::jumpToToday);

        Runnable transform = timelineScrollListener::transformChildren;
        int position = presenter.getDateTimePosition(startDate);
        if (position == 0) {
            // The second item from the right at the beginning of the
            // navigator is a special case. Because the item is already
            // visible without scrolling, the layout manager will try to
            // take a shortcut and not scroll at all. So we have to give
            // a specific offset if we want it to be centered. Of course,
            // we can only get the offset after layout.

            layoutManager.setOnPostLayout(() -> {
                layoutManager.setOnPostLayout(transform);
                layoutManager.scrollToPositionWithOffset(position, -layoutManager.getItemWidth());
                recyclerView.postDelayed(transform, 100); // delay = post-layout
            });
        } else {
            layoutManager.setOnPostLayout(transform);
            layoutManager.scrollToPosition(position);
        }

        return view;
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


    class TimelineScrollListener extends RecyclerView.OnScrollListener {
        final float BASE_SCALE = 0.8f;
        final float BASE_SCALE_REMAINDER = 0.2f;
        final float ACTIVE_DISTANCE = getResources().getDimensionPixelSize(R.dimen.timeline_navigator_active_distance);

        int previousState = RecyclerView.SCROLL_STATE_IDLE;

        public void transformChildren() {
            // 1:1 port of Delisa's iOS code

            float itemWidth = layoutManager.getItemWidth();
            float centerX = recyclerView.getMeasuredWidth() / 2f;
            for (int i = 0, size = recyclerView.getChildCount(); i < size; i++) {
                View child = recyclerView.getChildAt(i);

                float childCenter = child.getRight() - itemWidth / 2f;
                float childDistance = Math.abs(centerX - childCenter);
                float percentage = 1f / (childDistance / ACTIVE_DISTANCE);

                float alpha = Math.max(percentage, 0.4f);
                child.setAlpha(alpha);

                if (childDistance > ACTIVE_DISTANCE) {
                    float scale = BASE_SCALE + (BASE_SCALE_REMAINDER * percentage);
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                }
            }

            TimelineNavigatorAdapter.ItemViewHolder holder = (TimelineNavigatorAdapter.ItemViewHolder) recyclerView.findViewHolderForLayoutPosition(layoutManager.findLastVisibleItemPosition());
            if (holder.date != null) {
                monthText.setText(holder.date.toString("MMMM"));
            } else {
                monthText.setText(null);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            transformChildren();
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
            } else {
                transformChildren();
            }
        }
    }


    public interface OnTimelineDateSelectedListener {
        void onTimelineSelected(@NonNull DateTime date, @Nullable Timeline timeline);
    }
}
