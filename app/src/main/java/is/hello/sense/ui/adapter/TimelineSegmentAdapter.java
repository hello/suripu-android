package is.hello.sense.ui.adapter;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.ui.widget.HorizontalBarGraphView;

public class TimelineSegmentAdapter extends ArrayRecyclerAdapter<TimelineSegment, TimelineSegmentAdapter.SegmentViewHolder> implements View.OnClickListener {
    public static final int VIEW_ITEM_TYPE = 0;

    private static final int NUMBER_HOURS_ON_SCREEN = 20;

    private final Context context;
    private final LayoutInflater inflater;
    private final OnItemClickedListener onItemClickedListener;

    private final int itemHeight;
    private final int eventImageHeight;
    private final int stripeCornerRadius;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context, @NonNull OnItemClickedListener onItemClickedListener) {
        super();

        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.onItemClickedListener = onItemClickedListener;

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.itemHeight = size.y / NUMBER_HOURS_ON_SCREEN;
        this.eventImageHeight = context.getResources().getDimensionPixelSize(R.dimen.event_image_height);
        this.stripeCornerRadius = context.getResources().getDimensionPixelOffset(R.dimen.timeline_stripe_corner_radius);

        setHasStableIds(true);
    }

    //endregion


    //region Bindings

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            addAll(segments);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void handleError(@NonNull Throwable ignored) {
        clear();
    }

    private int calculateHeight(int position, @NonNull TimelineSegment segment) {
        if (segment.getEventType() != null) {
            int itemHeight = this.eventImageHeight + (this.itemHeight * 2);
            return (int) (Math.ceil(segment.getDuration() / 3600f) * itemHeight);
        } else if (position == 0 || position == getItemCount() - 1) {
            int itemHeight = this.stripeCornerRadius;
            return (int) (Math.ceil(segment.getDuration() / 3600f) * itemHeight);
        } else {
            return (int) ((segment.getDuration() / 3600f) * this.itemHeight);
        }
    }

    @Override
    public SegmentViewHolder onCreateViewHolder(ViewGroup viewGroup, int itemType) {
        View segmentView = inflater.inflate(R.layout.item_timeline_segment, viewGroup, false);
        segmentView.setOnClickListener(this);
        return new SegmentViewHolder(segmentView);
    }

    @Override
    public void onBindViewHolder(SegmentViewHolder segmentViewHolder, int position) {
        ItemPosition segmentPosition = ItemPosition.MIDDLE;
        if (position == 0)
            segmentPosition = ItemPosition.FIRST;
        else if (position == getItemCount() - 1)
            segmentPosition = ItemPosition.LAST;

        TimelineSegment segment = getItem(position);
        segmentViewHolder.displaySegment(segment, segmentPosition);
        segmentViewHolder.itemView.setMinimumHeight(calculateHeight(position, segment));
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_ITEM_TYPE;
    }

    public final class SegmentViewHolder extends RecyclerView.ViewHolder {
        private HorizontalBarGraphView graphView;
        private View stripe;
        private ImageView eventTypeImage;
        private TextView eventType;

        public SegmentViewHolder(View itemView) {
            super(itemView);

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
            graphView.setFillColor(context.getResources().getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));
            graphView.setValue(sleepDepth);

            int colorRes = Styles.getSleepDepthColorRes(sleepDepth);
            if (position == ItemPosition.FIRST) {
                float[] radii = {
                        stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                        0f, 0f, 0f, 0f,
                };
                stripe.setBackground(createRoundedDrawable(context.getResources().getColor(colorRes), radii));
            } else if (position == ItemPosition.LAST) {
                float[] radii = {
                        0f, 0f, 0f, 0f,
                        stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                };
                stripe.setBackground(createRoundedDrawable(context.getResources().getColor(colorRes), radii));
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


    @Override
    public void onClick(@NonNull View view) {
        int position = (Integer) view.getTag();
        onItemClickedListener.onItemClicked(position);
    }

    public interface OnItemClickedListener {
        void onItemClicked(int position);
    }
}
