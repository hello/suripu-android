package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineSegmentView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;

public class LegacyTimelineAdapter extends AbstractTimelineAdapter {
    private final TimelineSegmentView.Invariants invariants;

    //region Lifecycle

    public LegacyTimelineAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, dateFormatter);

        Resources resources = context.getResources();

        this.invariants = new TimelineSegmentView.Invariants(resources);
    }

    //endregion



    //region Bindings

    @Override
    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            buildItemInfoCache(segments);
            addAll(segments);
        }
    }

    @Override
    @SuppressWarnings("UnusedParameters")
    public void handleError(@NonNull Throwable ignored) {
        clear();
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
            if (isTimePosition(position)) {
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
