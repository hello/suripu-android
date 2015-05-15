package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.timeline.TimelineSegmentDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.StateSafeExecutor;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineBaseViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SEGMENT = 1;
    private static final int VIEW_TYPE_EVENT = 2;

    public static final int STATIC_ITEM_COUNT = 1;


    private final Context context;
    private final LayoutInflater inflater;
    private final View headerView;
    private final DateFormatter dateFormatter;

    private final int segmentMinHeight;
    private final int segmentHeightPerHour;
    private final int eventVerticalInset;

    private final List<TimelineSegment> segments = new ArrayList<>();
    private final Set<Integer> positionsWithTime = new HashSet<>();
    private final SparseArray<Pair<Integer, Integer>> stolenSleepDepths = new SparseArray<>();
    private int[] segmentHeights;

    private boolean use24Time = false;
    private @Nullable StateSafeExecutor onItemClickExecutor;
    private @Nullable OnItemClickListener onItemClickListener;

    public TimelineAdapter(@NonNull Context context,
                           @NonNull View headerView,
                           @NonNull DateFormatter dateFormatter) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.headerView = headerView;
        this.dateFormatter = dateFormatter;

        Resources resources = context.getResources();
        this.segmentMinHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        this.segmentHeightPerHour = resources.getDimensionPixelSize(R.dimen.timeline_segment_height_per_hour);
        this.eventVerticalInset = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_vertical_inset);
    }


    //region Rendering Cache

    private int calculateSegmentHeight(@NonNull TimelineSegment segment) {
        float hours = segment.getDuration() / 3600f;
        return Math.max(segmentMinHeight, Math.round(segmentHeightPerHour * hours));
    }

    private void buildCache() {
        int segmentCount = segments.size();
        this.segmentHeights = new int[segmentCount];
        stolenSleepDepths.clear();
        positionsWithTime.clear();

        Set<Integer> hours = new HashSet<>();
        for (int i = 0; i < segmentCount; i++) {
            TimelineSegment segment = segments.get(i);

            if (segment.hasEventInfo()) {
                int previousDepth = i > 0 ? segments.get(i - 1).getSleepDepth() : 0;
                int nextDepth = i < (segmentCount - 1) ? segments.get(i + 1).getSleepDepth() : 0;
                stolenSleepDepths.put(i + STATIC_ITEM_COUNT, Pair.create(previousDepth, nextDepth));

                this.segmentHeights[i] = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                int segmentHeight = calculateSegmentHeight(segment);
                this.segmentHeights[i] = segmentHeight;
            }

            int hour = segment.getShiftedTimestamp().getHourOfDay();
            if (!hours.contains(hour) && !positionsWithTime.contains(hour)) {
                positionsWithTime.add(i + STATIC_ITEM_COUNT);
                hours.add(hour);
            }
        }
    }

    private int getSegmentHeight(int adapterPosition) {
        return segmentHeights[adapterPosition - STATIC_ITEM_COUNT];
    }

    private void clearCache() {
        this.segmentHeights = null;
        stolenSleepDepths.clear();
        positionsWithTime.clear();
    }

    //endregion


    //region Data

    @Override
    public int getItemCount() {
        return STATIC_ITEM_COUNT + segments.size();
    }

    public TimelineSegment getSegment(int adapterPosition) {
        return segments.get(adapterPosition - STATIC_ITEM_COUNT);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (getSegment(position).hasEventInfo()) {
            return VIEW_TYPE_EVENT;
        } else {
            return VIEW_TYPE_SEGMENT;
        }
    }

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;

        for (int positionWithTime : positionsWithTime) {
            notifyItemChanged(positionWithTime);
        }
    }

    public void bindSegments(@Nullable List<TimelineSegment> newSegments) {
        int oldSize = segments.size();
        int newSize = newSegments != null ? newSegments.size() : 0;

        segments.clear();
        if (!Lists.isEmpty(newSegments)) {
            segments.addAll(newSegments);
            buildCache();
        }

        if (oldSize > newSize) {
            notifyItemRangeInserted(STATIC_ITEM_COUNT + oldSize, oldSize - newSize);
            notifyItemRangeChanged(STATIC_ITEM_COUNT, oldSize);
        } else if (oldSize < newSize) {
            notifyItemRangeRemoved(STATIC_ITEM_COUNT + oldSize, newSize - oldSize);
            notifyItemRangeChanged(STATIC_ITEM_COUNT, newSize);
        } else {
            notifyItemRangeChanged(STATIC_ITEM_COUNT, newSize);
        }
    }

    public void clear() {
        int oldSize = segments.size();
        segments.clear();
        clearCache();
        notifyItemRangeRemoved(STATIC_ITEM_COUNT, oldSize);
    }

    //endregion


    //region Click Support

    public void setOnItemClickListener(@Nullable StateSafeExecutor onItemClickExecutor,
                                       @Nullable OnItemClickListener onItemClickListener) {
        this.onItemClickExecutor = onItemClickExecutor;
        this.onItemClickListener = onItemClickListener;
    }

    private void dispatchItemClick(@NonNull SegmentViewHolder holder) {
        if (onItemClickExecutor != null && onItemClickListener != null) {
            OnItemClickListener onItemClickListener = this.onItemClickListener;
            onItemClickExecutor.execute(() -> {
                int position = holder.getAdapterPosition();
                TimelineSegment segment = getSegment(position);
                if (segment.hasEventInfo()) {
                    onItemClickListener.onEventItemClicked(position, segment);
                } else {
                    onItemClickListener.onSegmentItemClicked(position, segment);
                }
            });
        }
    }

    //endregion


    //region Vending Views

    @Override
    public TimelineBaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                return new StaticViewHolder(headerView);
            }

            case VIEW_TYPE_SEGMENT: {
                View segmentView = new View(context);
                segmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new SegmentViewHolder(segmentView);
            }

            case VIEW_TYPE_EVENT: {
                View segmentView = inflater.inflate(R.layout.item_timeline_segment, parent, false);
                return new EventViewHolder(segmentView);
            }

            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(TimelineBaseViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onViewRecycled(TimelineBaseViewHolder holder) {
        holder.unbind();
    }

    static class StaticViewHolder extends TimelineBaseViewHolder {
        StaticViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void bind(int position) {
            // Do nothing.
        }
    }

    class SegmentViewHolder extends TimelineBaseViewHolder implements View.OnClickListener {
        final TimelineSegmentDrawable drawable;

        SegmentViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            this.drawable = new TimelineSegmentDrawable(context);
            prepareDrawable();
            itemView.setBackground(drawable);
        }

        //region Binding

        @Override
        public final void bind(int position) {
            itemView.getLayoutParams().height = TimelineAdapter.this.getSegmentHeight(position);

            TimelineSegment segment = getSegment(position);
            bindSegment(position, segment);
        }

        void prepareDrawable() {
            drawable.setChildDrawable(itemView.getBackground());
        }

        void bindSegment(int position, @NonNull TimelineSegment segment) {
            drawable.setSleepDepth(segment.getSleepDepth());
            if (positionsWithTime.contains(position)) {
                drawable.setTimestamp(dateFormatter.formatForTimelineSegment(segment.getShiftedTimestamp(), use24Time));
            } else {
                drawable.setTimestamp(null);
            }
        }

        //endregion


        //region Click Support

        @Override
        public void onClick(View ignored) {
            dispatchItemClick(this);
        }

        //endregion
    }

    class EventViewHolder extends SegmentViewHolder {
        final TextView messageText;
        final TextView dateText;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setPivotX(0f);

            this.messageText = (TextView) itemView.findViewById(R.id.item_timeline_segment_message);
            this.dateText = (TextView) itemView.findViewById(R.id.item_timeline_segment_date);
        }

        //region Binding

        @Override
        void prepareDrawable() {
            drawable.setChildDrawablePadding(0, eventVerticalInset, 0, eventVerticalInset);

            super.prepareDrawable();
        }

        @Override
        void bindSegment(int position, @NonNull TimelineSegment segment) {
            super.bindSegment(position, segment);

            Pair<Integer, Integer> stolenScores = stolenSleepDepths.get(position);
            if (stolenScores != null) {
                drawable.setStolenTopSleepDepth(stolenScores.first);
                drawable.setStolenBottomSleepDepth(stolenScores.second);
            } else {
                drawable.setStolenBottomSleepDepth(0);
                drawable.setStolenTopSleepDepth(0);
            }

            int iconRes = Styles.getTimelineSegmentIconRes(segment);
            messageText.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
            messageText.setText(segment.getMessage());
            dateText.setText(dateFormatter.formatForTimelineEvent(segment.getShiftedTimestamp(), use24Time));
        }

        //endregion
    }

    //endregion


    public interface OnItemClickListener {
        void onSegmentItemClicked(int position, @NonNull TimelineSegment segment);
        void onEventItemClicked(int position, @NonNull TimelineSegment segment);
    }
}
