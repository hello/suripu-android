package is.hello.sense.ui.adapter;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineSegmentView;

public class TimelineSegmentAdapter extends RecyclerView.Adapter<TimelineSegmentAdapter.SegmentViewHolder> implements View.OnClickListener {
    private static final int NUMBER_HOURS_ON_SCREEN = 20;

    private final Context context;
    private final List<TimelineSegment> data = new ArrayList<>();
    private final OnItemClickedListener onItemClickedListener;

    private final int itemHeight;
    private final int eventImageHeight;
    private final int stripeCornerRadius;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context, @NonNull OnItemClickedListener onItemClickedListener) {
        super();

        this.context = context;
        this.onItemClickedListener = onItemClickedListener;

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.itemHeight = size.y / NUMBER_HOURS_ON_SCREEN;
        this.eventImageHeight = context.getResources().getDimensionPixelSize(R.dimen.event_image_height);
        this.stripeCornerRadius = context.getResources().getDimensionPixelOffset(R.dimen.timeline_stripe_corner_radius);
    }

    //endregion


    //region Bindings

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        notifyItemRangeRemoved(0, data.size());
        data.clear();

        if (segments != null) {
            data.addAll(segments);
            notifyItemRangeInserted(0, data.size());
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void handleError(@NonNull Throwable ignored) {
        notifyItemRangeRemoved(0, data.size());
        data.clear();
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
    public SegmentViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        TimelineSegmentView segmentView = new TimelineSegmentView(context);
        segmentView.setOnClickListener(this);
        return new SegmentViewHolder(segmentView);
    }

    @Override
    public void onBindViewHolder(SegmentViewHolder segmentViewHolder, int position) {
        TimelineSegmentView view = (TimelineSegmentView) segmentViewHolder.itemView;

        TimelineSegmentView.Position segmentPosition = TimelineSegmentView.Position.MIDDLE;
        if (position == 0)
            segmentPosition = TimelineSegmentView.Position.FIRST;
        else if (position == getItemCount() - 1)
            segmentPosition = TimelineSegmentView.Position.LAST;

        TimelineSegment segment = getItem(position);
        view.setTag(position);
        view.displaySegment(segment, segmentPosition);
        view.setLayoutParams(new RecyclerView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, calculateHeight(position, segment)));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public TimelineSegment getItem(int position) {
        return data.get(position);
    }


    public final class SegmentViewHolder extends RecyclerView.ViewHolder {
        public SegmentViewHolder(View itemView) {
            super(itemView);
        }
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
