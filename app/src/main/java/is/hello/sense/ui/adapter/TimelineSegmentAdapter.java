package is.hello.sense.ui.adapter;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.TimelineSegmentView;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    private final int baseItemHeight;
    private final int itemEventImageHeight;
    private final int stripeCornerRadius;

    private float[] itemHeights;
    private float totalItemHeight;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context) {
        super(context, R.layout.item_simple_text);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.baseItemHeight = size.y / Styles.TIMELINE_HOURS_ON_SCREEN;
        this.itemEventImageHeight = context.getResources().getDimensionPixelSize(R.dimen.event_image_height);
        this.stripeCornerRadius = context.getResources().getDimensionPixelOffset(R.dimen.view_timeline_segment_corner_radius);
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
        } else if (position == 0 || position == getCount() - 1) {
            int itemHeight = this.stripeCornerRadius;
            return (int) (Math.ceil(segment.getDuration() / 3600f) * itemHeight);
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


    //region Bindings

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            calculateItemHeights(segments);
            addAll(segments);
        } else {
            this.itemHeights = null;
            this.totalItemHeight = 0;
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void handleError(@NonNull Throwable ignored) {
        clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimelineSegmentView view = (TimelineSegmentView) convertView;
        if (view == null) {
            view = new TimelineSegmentView(parent.getContext());
            view.setTag(new SegmentViewHolder(view));
        }

        SegmentViewHolder segmentViewHolder = (SegmentViewHolder) view.getTag();

        TimelineSegmentView.StripeInset segmentPosition = TimelineSegmentView.StripeInset.NONE;
        if (position == 0) {
            segmentPosition = TimelineSegmentView.StripeInset.TOP;
        } else if (position == getCount() - 1) {
            segmentPosition = TimelineSegmentView.StripeInset.BOTTOM;
        }

        TimelineSegment segment = getItem(position);
        segmentViewHolder.displaySegment(segment, segmentPosition);
        segmentViewHolder.itemView.setMinimumHeight((int) getItemHeight(position));

        return view;
    }

    final class SegmentViewHolder {
        final TimelineSegmentView itemView;

        SegmentViewHolder(@NonNull TimelineSegmentView itemView) {
            this.itemView = itemView;
        }

        //region Displaying Data

        public void displaySegment(@NonNull TimelineSegment segment, @NonNull TimelineSegmentView.StripeInset stripeInset) {
            int sleepDepth = segment.getSleepDepth() < 0 ? 0 : segment.getSleepDepth();
            itemView.setSleepDepth(sleepDepth);
            itemView.setStripeInset(stripeInset);
            itemView.setRounded(segment.getEventType() != null);
            itemView.setEventResource(Styles.getTimelineSegmentIconRes(segment));
        }
    }
}
