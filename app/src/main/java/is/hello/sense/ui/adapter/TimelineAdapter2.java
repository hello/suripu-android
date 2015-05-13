package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.timeline.TimelineSegmentDrawable;
import is.hello.sense.util.DateFormatter;

public class TimelineAdapter2 extends RecyclerView.Adapter<TimelineAdapter2.BaseViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SEGMENT = 1;
    private static final int VIEW_TYPE_EVENT = 2;

    private static final int EXTRA_COUNT = 1;

    @Inject DateFormatter dateFormatter;

    private final Context context;
    private final LayoutInflater inflater;
    private final View headerView;

    private final int segmentMinHeight;
    private final int segmentHeightPerHour;

    private final List<TimelineSegment> segments = new ArrayList<>();
    private final Set<Integer> positionsWithTime = new HashSet<>();
    private int[] segmentHeights;

    private boolean use24Time = false;

    public TimelineAdapter2(@NonNull Context context, @NonNull View headerView) {
        SenseApplication.getInstance().inject(this);

        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.headerView = headerView;

        Resources resources = context.getResources();
        this.segmentMinHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment2_min_height);
        this.segmentHeightPerHour = resources.getDimensionPixelSize(R.dimen.timeline_segment2_height_per_hour);
    }


    //region Rendering Cache

    private int calculateSegmentHeight(@NonNull TimelineSegment segment) {
        float hours = segment.getDuration() / 3600f;
        return Math.max(segmentMinHeight, Math.round(segmentHeightPerHour * hours));
    }

    private void buildCache() {
        int segmentCount = segments.size();

        positionsWithTime.clear();
        this.segmentHeights = new int[segmentCount];

        Set<Integer> hours = new HashSet<>();
        for (int i = 0; i < segmentCount; i++) {
            TimelineSegment segment = segments.get(i);

            int segmentHeight = calculateSegmentHeight(segment);
            this.segmentHeights[i] = segmentHeight;

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

    public void bind(@NonNull Timeline timeline) {
        segments.clear();

        if (!Lists.isEmpty(timeline.getSegments())) {
            segments.addAll(timeline.getSegments());
            buildCache();
        } else {
            clearCache();
        }


        notifyDataSetChanged();
    }

    public void clear() {
        segments.clear();
        clearCache();
        notifyDataSetChanged();
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
                View segmentView = inflater.inflate(R.layout.item_timeline_segment2, parent, false);
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
            drawable.setOverlayDrawable(itemView.getBackground());
            itemView.setBackground(drawable);
        }

        @Override
        final void bind(int position) {
            itemView.getLayoutParams().height = getSegmentHeight(position);

            TimelineSegment segment = getSegment(position);
            bindSegment(position, segment);
        }

        int getSegmentHeight(int position) {
            return TimelineAdapter2.this.getSegmentHeight(position);
        }

        void bindSegment(int position, @NonNull TimelineSegment segment) {
            drawable.setSleepDepth(segment.getSleepDepth());
            if (positionsWithTime.contains(position)) {
                drawable.setTimestamp(dateFormatter.formatAsTimelineTimestamp(segment.getShiftedTimestamp(), use24Time));
                drawable.setWantsDivider(true);
            } else {
                drawable.setTimestamp(null);
                drawable.setWantsDivider(false);
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
        int getSegmentHeight(int position) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        @Override
        void bindSegment(int position, @NonNull TimelineSegment segment) {
            super.bindSegment(position, segment);

            messageText.setText(segment.getMessage());
            dateText.setText(dateFormatter.formatAsTimelineStamp(segment.getShiftedTimestamp(), use24Time));
        }
    }

    //endregion
}
