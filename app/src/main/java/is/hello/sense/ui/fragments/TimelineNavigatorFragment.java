package is.hello.sense.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.TimelineNavigatorPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ObservableLinearLayoutManager;
import is.hello.sense.ui.widget.MiniTimelineView;
import is.hello.sense.ui.widget.TimelineItemDecoration;
import is.hello.sense.ui.widget.graphing.SimplePieDrawable;
import is.hello.sense.ui.widget.util.Styles;
import rx.Subscription;

public class TimelineNavigatorFragment extends InjectionFragment {
    public static final String TAG = TimelineNavigatorFragment.class.getSimpleName();

    private static final int NUMBER_ITEMS_ON_SCREEN = 3;
    private static final int TOTAL_DAYS = 366;
    private static final String ARG_START_DATE = TimelineNavigatorFragment.class.getName() + ".ARG_START_DATE";

    @Inject TimelineNavigatorPresenter presenter;

    private TextView monthText;
    private RecyclerView recyclerView;
    private ObservableLinearLayoutManager linearLayoutManager;
    private Adapter adapter;

    public static TimelineNavigatorFragment newInstance(@NonNull DateTime startTime) {
        TimelineNavigatorFragment fragment = new TimelineNavigatorFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_START_DATE, startTime);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DateTime startTime = (DateTime) getArguments().getSerializable(ARG_START_DATE);
        presenter.setStartTime(startTime);
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline_navigator, container, false);

        this.monthText = (TextView) view.findViewById(R.id.fragment_timeline_navigator_month);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_navigator_recycler_view);
        recyclerView.addItemDecoration(new TimelineItemDecoration(getResources(), R.drawable.graph_grid_fill, R.dimen.divider_size));

        ScrollListener scrollListener = new ScrollListener();
        recyclerView.setOnScrollListener(scrollListener);

        this.linearLayoutManager = new ObservableLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, true);
        linearLayoutManager.setOnPostLayout(scrollListener::transformChildren);
        recyclerView.setLayoutManager(linearLayoutManager);

        this.adapter = new Adapter(getActivity());
        recyclerView.setAdapter(adapter);

        Button todayButton = (Button) view.findViewById(R.id.fragment_timeline_navigator_today);
        todayButton.setOnClickListener(this::jumpToToday);

        return view;
    }


    public void jumpToToday(@NonNull View sender) {
        recyclerView.smoothScrollToPosition(0);
    }


    public int getItemWidth() {
        return recyclerView.getMeasuredWidth() / NUMBER_ITEMS_ON_SCREEN;
    }


    public void onItemClicked(@NonNull View itemView, int position) {
        int searchPosition = (recyclerView.getChildCount() == 2) ? 0 : 1;
        if (recyclerView.getChildAt(searchPosition) == itemView) {
            DateTime newDate = presenter.getStartTime().plusDays(-position);
            ((OnTimelineDateSelectedListener) getActivity()).onTimelineDateSelected(newDate);
        } else if (recyclerView.getChildAt(searchPosition + 1) == itemView) {
            recyclerView.smoothScrollBy(-getItemWidth(), 0);
        } else {
            recyclerView.smoothScrollBy(getItemWidth(), 0);
        }
    }


    class Adapter extends RecyclerView.Adapter<Adapter.ItemViewHolder> implements View.OnClickListener {
        private final LayoutInflater inflater;
        private final Set<ItemViewHolder> visibleHolders = new HashSet<>(4);
        private boolean suspended = false;

        Adapter(@NonNull Context context) {
            this.inflater = LayoutInflater.from(context);
        }


        void suspend() {
            this.suspended = true;
        }

        void resume() {
            this.suspended = false;
            for (ItemViewHolder holder : visibleHolders) {
                holder.load();
            }
        }


        @Override
        public int getItemCount() {
            return TOTAL_DAYS;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View itemView = inflater.inflate(R.layout.item_timeline_navigator, viewGroup, false);
            itemView.setOnClickListener(this);
            return new ItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            DateTime date = presenter.getStartTime().plusDays(-position);

            holder.itemView.setTag(position);

            holder.date = date;
            holder.dayNumber.setText(date.toString("d"));
            holder.dayName.setText(date.toString("EE"));
            holder.pieDrawable.setValue(0);
            holder.pieDrawable.setTrackColor(Styles.getSleepScoreBorderColor(getActivity(), 0));
            holder.score.setText(R.string.missing_data_placeholder);
            if (!suspended) {
                holder.load();
            }

            visibleHolders.add(holder);
        }

        @Override
        public void onViewRecycled(ItemViewHolder holder) {
            super.onViewRecycled(holder);

            holder.reset();
            visibleHolders.remove(holder);
        }


        @Override
        public void onClick(View view) {
            int position = (int) view.getTag();
            onItemClicked(view, position);
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            final TextView dayNumber;
            final TextView dayName;
            final TextView score;
            final MiniTimelineView timeline;

            final SimplePieDrawable pieDrawable;

            @Nullable DateTime date;
            @Nullable Subscription loading;

            ItemViewHolder(View itemView) {
                super(itemView);

                itemView.setMinimumWidth(getItemWidth());

                this.dayNumber = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_number);
                this.dayName = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_name);
                this.score = (TextView) itemView.findViewById(R.id.item_timeline_navigator_score);
                this.timeline = (MiniTimelineView) itemView.findViewById(R.id.item_timeline_navigator_timeline);

                this.pieDrawable = new SimplePieDrawable(getResources());

                View pieView = itemView.findViewById(R.id.item_timeline_navigator_pie);
                pieView.setBackground(pieDrawable);
            }

            void reset() {
                if (loading != null) {
                    loading.unsubscribe();
                    this.loading = null;
                }

                this.date = null;

                pieDrawable.setValue(0);
                pieDrawable.setTrackColor(getResources().getColor(R.color.border));
                score.setText(R.string.missing_data_placeholder);

                timeline.setTimelineSegments(null);
            }

            void load() {
                if (loading == null && date != null) {
                    this.loading = bindAndSubscribe(presenter.timelineForDate(date), timeline -> {
                        this.loading = null;

                        int sleepScore = timeline.getScore();
                        score.setText(Integer.toString(sleepScore));
                        pieDrawable.setTrackColor(Color.TRANSPARENT);
                        pieDrawable.setFillColor(Styles.getSleepScoreColor(getActivity(), sleepScore));
                        pieDrawable.setValue(sleepScore);

                        this.timeline.setTimelineSegments(timeline.getSegments());
                    }, error -> {
                        this.loading = null;

                        score.setText(R.string.missing_data_placeholder);
                        pieDrawable.setFillColor(getResources().getColor(R.color.sensor_warning));
                        pieDrawable.setValue(100);

                        timeline.setTimelineSegments(null);
                    });
                }
            }
        }
    }

    class ScrollListener extends RecyclerView.OnScrollListener {
        final float BASE_SCALE = 0.8f;
        final float ACTIVE_DISTANCE = getResources().getDimensionPixelSize(R.dimen.timeline_navigator_active_distance);

        int previousState = RecyclerView.SCROLL_STATE_IDLE;

        public void transformChildren() {
            // 1:1 port of Delisa's iOS code
            float itemWidth = getItemWidth();
            float centerX = recyclerView.getMeasuredWidth() / 2f;
            for (int i = 0, size = recyclerView.getChildCount(); i < size; i++) {
                View child = recyclerView.getChildAt(i);

                float childCenter = child.getRight() - itemWidth / 2f;
                float childDistance = Math.abs(centerX - childCenter);
                float percentage = 1f / (childDistance / ACTIVE_DISTANCE);

                float alpha = Math.max(percentage, 0.4f);
                child.setAlpha(alpha);

                if (childDistance > ACTIVE_DISTANCE) {
                    float scale = BASE_SCALE + (0.2f * percentage);
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                }
            }

            Adapter.ItemViewHolder holder = (Adapter.ItemViewHolder) recyclerView.findViewHolderForPosition(linearLayoutManager.findLastVisibleItemPosition());
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
                adapter.suspend();
            } else if (previousState != RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToNearestItem(recyclerView);
                adapter.resume();
            }

            this.previousState = newState;
        }

        public void snapToNearestItem(RecyclerView recyclerView) {
            int lastItem = linearLayoutManager.findLastVisibleItemPosition();
            int lastCompleteItem = linearLayoutManager.findLastCompletelyVisibleItemPosition();
            if (lastItem != lastCompleteItem) {
                View itemView = recyclerView.findViewHolderForPosition(lastItem).itemView;
                int width = itemView.getMeasuredWidth();
                int x = Math.abs(itemView.getRight());

                recyclerView.stopScroll();
                if (x > width / 2) {
                    recyclerView.smoothScrollToPosition(lastItem);
                } else {
                    recyclerView.smoothScrollToPosition(linearLayoutManager.findFirstVisibleItemPosition());
                }
            } else {
                transformChildren();
            }
        }
    }


    public interface OnTimelineDateSelectedListener {
        void onTimelineDateSelected(@NonNull DateTime date);
    }
}
