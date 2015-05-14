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

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.BaseViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SEGMENT = 1;
    private static final int VIEW_TYPE_EVENT = 2;

    private static final int EXTRA_COUNT = 1;


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

        Set<Integer> hours = new HashSet<>();
        for (int i = 0; i < segmentCount; i++) {
            TimelineSegment segment = segments.get(i);

            if (segment.hasEventInfo()) {
                int previousDepth = i > 0 ? segments.get(i - 1).getSleepDepth() : 0;
                int nextDepth = i < (segmentCount - 1) ? segments.get(i + 1).getSleepDepth() : 0;
                stolenSleepDepths.put(i + EXTRA_COUNT, Pair.create(previousDepth, nextDepth));

                this.segmentHeights[i] = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                int segmentHeight = calculateSegmentHeight(segment);
                this.segmentHeights[i] = segmentHeight;
            }

            int hour = segment.getShiftedTimestamp().getHourOfDay();
            if (!hours.contains(hour) && !positionsWithTime.contains(hour)) {
                positionsWithTime.add(i + EXTRA_COUNT);
                hours.add(hour);
            }
        }
    }

    private int getSegmentHeight(int adapterPosition) {
        return segmentHeights[adapterPosition - EXTRA_COUNT];
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
        return EXTRA_COUNT + segments.size();
    }

    public TimelineSegment getSegment(int adapterPosition) {
        return segments.get(adapterPosition - EXTRA_COUNT);
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
        clear();

        if (!Lists.isEmpty(newSegments)) {
            segments.addAll(newSegments);
            buildCache();
        }

        notifyItemRangeInserted(EXTRA_COUNT, segments.size() + EXTRA_COUNT);
    }

    public void clear() {
        int extent = segments.size();
        segments.clear();
        clearCache();
        notifyItemRangeRemoved(EXTRA_COUNT, extent + EXTRA_COUNT);
    }

    //endregion


    //region Vending Views

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                return new BaseViewHolder(headerView);
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
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }

    class BaseViewHolder extends RecyclerView.ViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind(int position) {
            // Do nothing.
        }
    }

    class SegmentViewHolder extends BaseViewHolder {
        final TimelineSegmentDrawable drawable;

        SegmentViewHolder(@NonNull View itemView) {
            super(itemView);

            this.drawable = new TimelineSegmentDrawable(context);
            prepareDrawable();
            itemView.setBackground(drawable);
        }

        @Override
        final void bind(int position) {
            itemView.getLayoutParams().height = TimelineAdapter.this.getSegmentHeight(position);

            TimelineSegment segment = getSegment(position);
            bindSegment(position, segment);
        }

        void prepareDrawable() {
            drawable.setOverlayDrawable(itemView.getBackground());
        }

        void bindSegment(int position, @NonNull TimelineSegment segment) {
            drawable.setSleepDepth(segment.getSleepDepth());
            if (positionsWithTime.contains(position)) {
                drawable.setTimestamp(dateFormatter.formatForTimelineSegment(segment.getShiftedTimestamp(), use24Time));
            } else {
                drawable.setTimestamp(null);
            }
        }
    }

    class EventViewHolder extends SegmentViewHolder {
        final TextView messageText;
        final TextView dateText;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            this.messageText = (TextView) itemView.findViewById(R.id.item_timeline_segment_message);
            this.dateText = (TextView) itemView.findViewById(R.id.item_timeline_segment_date);
        }

        @Override
        void prepareDrawable() {
            drawable.setOverlayInsets(0, eventVerticalInset, 0, eventVerticalInset);

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
    }

    //endregion
}
