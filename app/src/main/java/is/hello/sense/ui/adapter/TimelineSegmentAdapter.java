package is.hello.sense.ui.adapter;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.ui.widget.HorizontalBarGraphView;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    private static final int NUMBER_HOURS_ON_SCREEN = 20;

    private final LayoutInflater inflater;

    private final int baseItemHeight;
    private final int itemEventImageHeight;
    private final int stripeCornerRadius;

    private float[] itemHeights;
    private float totalItemHeight;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context) {
        super(context, R.layout.item_timeline_segment);

        this.inflater = LayoutInflater.from(context);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.baseItemHeight = size.y / NUMBER_HOURS_ON_SCREEN;
        this.itemEventImageHeight = context.getResources().getDimensionPixelSize(R.dimen.event_image_height);
        this.stripeCornerRadius = context.getResources().getDimensionPixelOffset(R.dimen.timeline_stripe_corner_radius);
    }

    //endregion


    //region Item Heights

    private int calculateHeight(int position, @NonNull TimelineSegment segment) {
        if (segment.getEventType() != null) {
            int itemHeight = this.itemEventImageHeight + (this.baseItemHeight * 2);
            return (int) (Math.ceil(segment.getDuration() / 3600f) * itemHeight);
        } else if (position == 0 || position == getCount() - 1) {
            int itemHeight = this.stripeCornerRadius;
            return (int) (Math.ceil(segment.getDuration() / 3600f) * itemHeight);
        } else {
            return (int) ((segment.getDuration() / 3600f) * this.baseItemHeight);
        }
    }

    private void calculateItemHeights(@NonNull List<TimelineSegment> segments) {
        this.itemHeights = new float[segments.size()];
        this.totalItemHeight = 0;

        for (int i = 0, size = itemHeights.length; i < size; i++) {
            int height = calculateHeight(i, segments.get(i));
            this.itemHeights[i] = height;
            this.totalItemHeight += height;
        }
    }

    public float getTotalItemHeight() {
        return totalItemHeight;
    }

    public float getItemHeight(int position) {
        return itemHeights[position];
    }

    public float getHeightOfItems(int start, int end, float endScaleFactor) {
        if (start == end) {
            return (itemHeights[end] * endScaleFactor);
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
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_timeline_segment, parent, false);
            view.setTag(new SegmentViewHolder(view));
        }

        SegmentViewHolder segmentViewHolder = (SegmentViewHolder) view.getTag();

        ItemPosition segmentPosition = ItemPosition.MIDDLE;
        if (position == 0) {
            segmentPosition = ItemPosition.FIRST;
        } else if (position == getCount() - 1) {
            segmentPosition = ItemPosition.LAST;
        }

        TimelineSegment segment = getItem(position);
        segmentViewHolder.displaySegment(segment, segmentPosition);
        segmentViewHolder.itemView.setMinimumHeight((int) getItemHeight(position));

        return view;
    }

    final class SegmentViewHolder {
        final View itemView;

        final HorizontalBarGraphView graphView;
        final View stripe;
        final ImageView eventTypeImage;
        final TextView eventType;

        SegmentViewHolder(@NonNull View itemView) {
            this.itemView = itemView;
            this.graphView = (HorizontalBarGraphView) itemView.findViewById(R.id.view_timeline_segment_graph);
            this.stripe = itemView.findViewById(R.id.view_timeline_segment_event_stripe);
            this.eventTypeImage = (ImageView) itemView.findViewById(R.id.view_timeline_segment_image_event_type);
            this.eventType = (TextView) itemView.findViewById(R.id.view_timeline_segment_event_type);
        }

        //region Displaying Data

        public @NonNull Drawable createRoundedDrawable(int color, float[] radii) {
            RoundRectShape shape = new RoundRectShape(radii, null, null);
            ShapeDrawable drawable = new ShapeDrawable(shape);
            drawable.getPaint().setColor(color);
            return drawable;
        }

        public void displaySegment(@NonNull TimelineSegment segment, @NonNull ItemPosition position) {
            int sleepDepth = segment.getSleepDepth() < 0 ? 0 : segment.getSleepDepth();
            graphView.setFillColor(getContext().getResources().getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));
            graphView.setValue(sleepDepth);

            int colorRes = Styles.getSleepDepthColorRes(sleepDepth);
            if (position == ItemPosition.FIRST) {
                float[] radii = {
                        stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                        0f, 0f, 0f, 0f,
                };
                stripe.setBackground(createRoundedDrawable(getContext().getResources().getColor(colorRes), radii));
            } else if (position == ItemPosition.LAST) {
                float[] radii = {
                        0f, 0f, 0f, 0f,
                        stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                };
                stripe.setBackground(createRoundedDrawable(getContext().getResources().getColor(colorRes), radii));
            } else {
                stripe.setBackgroundResource(colorRes);
            }

            if (segment.getEventType() != null) {
                eventTypeImage.setImageResource(segment.getEventType().iconRes);
                eventType.setText(segment.getEventType().nameString);

                eventTypeImage.setVisibility(View.VISIBLE);
                eventType.setVisibility(View.VISIBLE);
            } else {
                eventTypeImage.setImageDrawable(null);

                eventTypeImage.setVisibility(View.GONE);
                eventType.setVisibility(View.GONE);
            }
        }
    }

    public enum ItemPosition {
        FIRST,
        MIDDLE,
        LAST,
    }
}
