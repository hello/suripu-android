package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineSegmentView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    private final DateFormatter dateFormatter;

    private final TimelineSegmentView.Invariants invariants;
    private final int baseItemHeight;
    private final int itemEventImageHeight;

    private final Set<Integer> timePositions = new HashSet<>();
    private float[] itemHeights;

    private boolean use24Time;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, R.layout.item_simple_text);

        this.dateFormatter = dateFormatter;

        Resources resources = context.getResources();

        this.invariants = new TimelineSegmentView.Invariants(resources);

        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.baseItemHeight = Math.max(minItemHeight, size.y / Styles.TIMELINE_HOURS_ON_SCREEN);
        this.itemEventImageHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_image_height);
    }

    //endregion


    //region Item Info

    /**
     * Calculates the height required to display the item at a given position.
     */
    private int calculateItemHeight(@NonNull TimelineSegment segment) {
        if (segment.hasEventInfo()) {
            return (int) (Math.ceil(segment.getDuration() / 3600f) * this.itemEventImageHeight);
        } else {
            return (int) ((segment.getDuration() / 3600f) * this.baseItemHeight);
        }
    }

    /**
     * Calculates heights for all items in the adapter, and determines
     * which items are to be used as representative times.
     */
    private void buildItemInfoCache(@NonNull List<TimelineSegment> segments) {
        this.itemHeights = new float[segments.size()];
        timePositions.clear();

        Set<Integer> hoursRepresented = new HashSet<>();
        for (int i = 0, size = itemHeights.length; i < size; i++) {
            int height = calculateItemHeight(segments.get(i));
            this.itemHeights[i] = height;

            TimelineSegment segment = segments.get(i);
            int hour = segment.getShiftedTimestamp().getHourOfDay();
            if (hoursRepresented.contains(hour)) {
                continue;
            }

            timePositions.add(i);
            hoursRepresented.add(hour);
        }
    }

    /**
     * Clears all of the cached item information in the adapter.
     */
    private void clearItemInfoCache() {
        this.itemHeights = null;
        timePositions.clear();
    }

    /**
     * Returns the height of the item at a given position in the adapter.
     * <p/>
     * Returns in Constant time.
     */
    public float getItemHeight(int position) {
        return itemHeights[position];
    }

    //endregion


    //region Bindings

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            buildItemInfoCache(segments);
            addAll(segments);
        } else {
            clearItemInfoCache();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void handleError(@NonNull Throwable ignored) {
        clear();
        clearItemInfoCache();
    }

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;
        notifyDataSetChanged();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimelineSegmentView view = (TimelineSegmentView) convertView;
        if (view == null) {
            view = new TimelineSegmentView(parent.getContext(), invariants);
            view.setTag(new SegmentViewHolder(view));
        }

        SegmentViewHolder segmentViewHolder = (SegmentViewHolder) view.getTag();

        TimelineSegment segment = getItem(position);
        segmentViewHolder.displaySegment(position, segment);
        segmentViewHolder.itemView.setMinimumHeight((int) getItemHeight(position));

        return view;
    }

    final class SegmentViewHolder {
        final TimelineSegmentView itemView;

        SegmentViewHolder(@NonNull TimelineSegmentView itemView) {
            this.itemView = itemView;
        }

        //region Displaying Data

        public void displaySegment(int position, @NonNull TimelineSegment segment) {
            int sleepDepth = segment.getSleepDepth() < 0 ? 0 : segment.getSleepDepth();
            itemView.setSleepDepth(sleepDepth, segment.isBeforeSleep());
            itemView.setEventResource(Styles.getTimelineSegmentIconRes(segment));

            DateTime segmentTimestamp = segment.getShiftedTimestamp();
            if (timePositions.contains(position)) {
                DateTime time = segmentTimestamp.withMinuteOfHour(0);
                itemView.setLeftTime(dateFormatter.formatAsTimelineStamp(time, use24Time));
            } else {
                itemView.setLeftTime(null);
            }

            if (segment.hasEventInfo()) {
                itemView.setRightTime(dateFormatter.formatAsTimelineStamp(segmentTimestamp, use24Time));
            } else {
                itemView.setRightTime(null);
            }
        }
    }
}
