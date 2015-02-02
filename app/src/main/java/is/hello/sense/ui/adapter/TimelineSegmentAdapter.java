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

import static is.hello.sense.api.model.TimelineSegment.EventType;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    private final DateFormatter dateFormatter;

    private final int baseItemHeight;
    private final int minTimeItemHeight;
    private final int itemEventImageHeight;

    private float[] itemHeights;
    private float totalItemHeight;

    private Set<Integer> positionsWithTime = new HashSet<>();

    private boolean use24Time;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, R.layout.item_simple_text);

        this.dateFormatter = dateFormatter;

        Resources resources = context.getResources();
        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.baseItemHeight = Math.max(minItemHeight, size.y / Styles.TIMELINE_HOURS_ON_SCREEN);

        this.minTimeItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height_time);
        this.itemEventImageHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_image_height);
    }

    //endregion


    //region Item Heights

    /**
     * Calculates the height required to display the item at a given position.
     */
    private int calculateItemHeight(int position, @NonNull TimelineSegment segment) {
        if (segment.getEventType() != null) {
            int itemHeight = this.itemEventImageHeight + this.baseItemHeight;
            return (int) (Math.ceil(segment.getDuration() / 3600f) * itemHeight);
        } else if (positionsWithTime.contains(position)) {
            int rawHeight = (int) ((segment.getDuration() / 3600f) * (this.baseItemHeight * 2f));
            return Math.max(minTimeItemHeight, rawHeight);
        } else {
            return (int) ((segment.getDuration() / 3600f) * this.baseItemHeight);
        }
    }

    /**
     * Calculates the sizing information for a given list of timeline
     * segments, and caches it into the adapter for later use.
     */
    private void calculateItemHeights(@NonNull List<TimelineSegment> segments) {
        this.itemHeights = new float[segments.size()];
        this.totalItemHeight = 0;

        for (int i = 0, size = itemHeights.length; i < size; i++) {
            int height = calculateItemHeight(i, segments.get(i));
            this.itemHeights[i] = height;
            this.totalItemHeight += height;
        }
    }

    /**
     * Returns the total height of all of the items contained in the adapter.
     * <p/>
     *
     * Returns in Constant time.
     */
    public float getTotalItemHeight() {
        return totalItemHeight;
    }

    /**
     * Returns the height of the item at a given position in the adapter.
     * <p/>
     * Returns in Constant time.
     */
    public float getItemHeight(int position) {
        return itemHeights[position];
    }

    /**
     * Calculates the total height of a series of items contained in the range {start, end} <em>inclusive</em>.
     *
     * @param start The index of the first item in the range.
     * @param end The index of the last item in the range, included in the final sum.
     * @param endScaleFactor The amount of the last item that is currently visible.
     */
    public float getHeightOfItems(int start, int end, float endScaleFactor) {
        if (start == end) {
            return itemHeights[start] * endScaleFactor;
        } else {
            float sum = 0;
            for (int i = start; i < end; i++) {
                sum += itemHeights[i];
            }
            sum += (itemHeights[end] * endScaleFactor);

            return sum;
        }
    }

    //endregion


    //region Representative Time Indexes

    private void calculatePositionsWithTime(@NonNull List<TimelineSegment> segments) {
        Set<Integer> hoursRepresented = new HashSet<>();
        for (int i = 1; i < segments.size(); i++) {
            TimelineSegment segment = segments.get(i);
            int hour = segment.getShiftedTimestamp().getHourOfDay();
            if (hoursRepresented.contains(hour)) {
                continue;
            }

            positionsWithTime.add(i);
            hoursRepresented.add(hour);
        }
    }

    //endregion


    //region Bindings

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            calculatePositionsWithTime(segments);
            calculateItemHeights(segments);
            addAll(segments);
        } else {
            this.itemHeights = null;
            this.totalItemHeight = 0;

            positionsWithTime.clear();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void handleError(@NonNull Throwable ignored) {
        clear();
    }

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;
        notifyDataSetChanged();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimelineSegmentView view = (TimelineSegmentView) convertView;
        if (view == null) {
            view = new TimelineSegmentView(parent.getContext());
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
            itemView.setSleepDepth(sleepDepth);
            itemView.setEventResource(Styles.getTimelineSegmentIconRes(segment));

            DateTime segmentTimestamp = segment.getShiftedTimestamp();
            if (positionsWithTime.contains(position)) {
                DateTime time = segmentTimestamp.withMinuteOfHour(0);
                itemView.setLeftTime(dateFormatter.formatAsTimelineStamp(time, use24Time));
            } else {
                itemView.setLeftTime(null);
            }

            EventType eventType = segment.getEventType();
            if (eventType != null) {
                itemView.setRightTime(dateFormatter.formatAsTimelineStamp(segmentTimestamp, use24Time));
            } else {
                itemView.setRightTime(null);
            }
        }
    }
}
