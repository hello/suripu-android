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

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.Views;

public class TimelineNavigatorFragment extends InjectionFragment {
    public static final String TAG = TimelineNavigatorFragment.class.getSimpleName();

    private static final int NUMBER_ITEMS_ON_SCREEN = 3;
    private static final String ARG_START_DATE = TimelineNavigatorFragment.class.getName() + ".ARG_START_DATE";

    private DateTime startTime;

    private TextView monthText;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
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

        this.startTime = (DateTime) getArguments().getSerializable(ARG_START_DATE);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline_navigator, container, false);

        this.monthText = (TextView) view.findViewById(R.id.fragment_timeline_navigator_month);
        monthText.setText(startTime.toString("MMMM"));


        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_navigator_recycler_view);
        ScrollListener scrollListener = new ScrollListener();
        recyclerView.setOnScrollListener(scrollListener);
        Views.observeNextLayout(recyclerView).subscribe(ignored -> scrollListener.transformChildren());

        this.linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, true);
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
        private final Context context;
        private final LayoutInflater inflater;

        Adapter(@NonNull Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
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
            holder.day.setText(Integer.toString(position));
        }


        class ItemViewHolder extends RecyclerView.ViewHolder {
            final TextView day;
            final TextView month;

            ItemViewHolder(View itemView) {
                super(itemView);

                itemView.setMinimumWidth(getItemWidth());

                this.day = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day);
                this.month = (TextView) itemView.findViewById(R.id.item_timeline_navigator_month);
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

                float childCenter = child.getX() + itemWidth / 2f;
                float childDistance = Math.abs(centerX - childCenter);

                float percentage = Math.max(1f / (childDistance / ACTIVE_DISTANCE), 0.4f);
                child.setAlpha(percentage);

                if (childDistance > ACTIVE_DISTANCE) {
                    float scale = BASE_SCALE + 0.2f * (1f / (childDistance / ACTIVE_DISTANCE));
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            transformChildren();
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (previousState != RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToNearestItem(recyclerView);
            }

            this.previousState = newState;
        }

        public void snapToNearestItem(RecyclerView recyclerView) {
            int firstItem = linearLayoutManager.findFirstVisibleItemPosition();
            int firstCompleteItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
            if (firstItem < firstCompleteItem) {
                recyclerView.smoothScrollToPosition(firstItem);
            } else if (firstItem > firstCompleteItem) {
                recyclerView.smoothScrollToPosition(firstCompleteItem);
            }
        }
    }
}
