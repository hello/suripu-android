package is.hello.sense.ui.fragments;

import android.content.Context;
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
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelineNavigatorPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ObservableLinearLayoutManager;
import is.hello.sense.ui.widget.Styles;
import is.hello.sense.ui.widget.Views;
import is.hello.sense.ui.widget.graphing.SimplePieDrawable;
import rx.Observable;

public class TimelineNavigatorFragment extends InjectionFragment {
    public static final String TAG = TimelineNavigatorFragment.class.getSimpleName();

    private static final int NUMBER_ITEMS_ON_SCREEN = 3;
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

    }


    public int getItemWidth() {
        return recyclerView.getMeasuredWidth() / NUMBER_ITEMS_ON_SCREEN;
    }


    class Adapter extends RecyclerView.Adapter<Adapter.ItemViewHolder> {
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
                holder.loadTimeline();
            }
        }


        @Override
        public int getItemCount() {
            return 24;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            return new ItemViewHolder(inflater.inflate(R.layout.item_timeline_navigator, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            DateTime date = presenter.getStartTime().plusDays(-position);

            holder.date = date;
            holder.dayNumber.setText(date.toString("d"));
            holder.dayName.setText(date.toString("EE"));
            holder.loadTimeline();

            visibleHolders.add(holder);
        }

        @Override
        public void onViewRecycled(ItemViewHolder holder) {
            super.onViewRecycled(holder);

            holder.date = null;
            holder.timelineObservable = null;
            holder.loaded = false;

            visibleHolders.remove(holder);
        }


        class ItemViewHolder extends RecyclerView.ViewHolder {
            final TextView dayNumber;
            final TextView dayName;
            final TextView score;

            final SimplePieDrawable pieDrawable;

            @Nullable DateTime date;
            @Nullable Observable<Timeline> timelineObservable;
            boolean loaded = false;

            ItemViewHolder(View itemView) {
                super(itemView);

                itemView.setMinimumWidth(getItemWidth());

                this.dayNumber = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_number);
                this.dayName = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_name);
                this.score = (TextView) itemView.findViewById(R.id.item_timeline_navigator_score);

                this.pieDrawable = new SimplePieDrawable(getResources());

                View pieView = itemView.findViewById(R.id.item_timeline_navigator_pie);
                pieView.setBackground(pieDrawable);
            }

            void loadTimeline() {
                if (!suspended && timelineObservable == null && !loaded && date != null) {
                    this.timelineObservable = presenter.timelineForDate(date);
                    bindAndSubscribe(timelineObservable,
                                     t -> {
                                         this.timelineObservable = null;
                                         this.loaded = true;

                                         int sleepScore = t.getScore();
                                         score.setText(Integer.toString(sleepScore));
                                         pieDrawable.setTrackColor(Styles.getSleepScoreBorderColor(getActivity(), sleepScore));
                                         pieDrawable.setFillColor(Styles.getSleepScoreColor(getActivity(), sleepScore));
                                         pieDrawable.setValue(sleepScore);
                                     },
                                     e -> {
                                         this.timelineObservable = null;
                                         this.loaded = false;

                                         score.setText(R.string.missing_data_placeholder);
                                         pieDrawable.setFillColor(getResources().getColor(R.color.sensor_warning));
                                         pieDrawable.setValue(100);
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
}
