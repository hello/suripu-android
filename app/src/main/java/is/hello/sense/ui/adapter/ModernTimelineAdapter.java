package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineBarView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;

public class ModernTimelineAdapter extends AbstractTimelineAdapter {
    private static final int TYPE_BAR = 0;
    private static final int TYPE_EVENT = 1;
    private static final int TYPE_COUNT = 2;

    private final LayoutInflater inflater;
    private final Resources resources;

    public ModernTimelineAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, dateFormatter);

        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }


    //region Binding

    @Override
    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            buildItemInfoCache(segments);
            addAll(segments);
        }
    }

    @Override
    public void handleError(@NonNull Throwable e) {
        clear();
    }

    //endregion


    //region Views

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).hasEventInfo()) {
            return TYPE_EVENT;
        } else {
            return TYPE_BAR;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimelineSegment segment = getItem(position);
        View view = convertView;
        if (segment.hasEventInfo()) {
            if (view == null) {
                view = inflater.inflate(R.layout.item_timeline_event, parent, false);
                view.setTag(new EventViewHolder(view));
            }

            EventViewHolder holder = (EventViewHolder) view.getTag();
            holder.bind(position, segment);
        } else {
            if (view == null) {
                view = new TimelineBarView(getContext());
            }

            TimelineBarView barView = (TimelineBarView) view;
            if (segment.isBeforeSleep()) {
                barView.setEmpty();
            } else {
                barView.setSleepDepth(segment.getSleepDepth());
            }
            barView.setMinimumHeight((int) getItemHeight(position));
        }
        return view;
    }

    class EventViewHolder {
        final View itemView;
        final ImageView image;
        final TextView text;
        final TextView date;

        EventViewHolder(@NonNull View view) {
            this.itemView = view;
            this.image = (ImageView) view.findViewById(R.id.item_timeline_event_image);
            this.text = (TextView) view.findViewById(R.id.item_timeline_event_text);
            this.date = (TextView) view.findViewById(R.id.item_timeline_event_date);
        }

        void bind(int position, @NonNull TimelineSegment segment) {
            image.setImageResource(Styles.getTimelineSegmentIconRes(segment));
            image.setContentDescription(resources.getString(segment.getEventType().nameString));
            text.setText(segment.getMessage());

            date.setText(dateFormatter.formatAsTime(segment.getShiftedTimestamp(), use24Time));
            if (segment.isTimeAdjustable()) {
                date.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Light_Small_Link);
                date.setPaintFlags(date.getPaintFlags() | TextPaint.UNDERLINE_TEXT_FLAG);
            } else {
                date.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Light_Small_Dimmed);
                date.setPaintFlags(date.getPaintFlags() & ~TextPaint.UNDERLINE_TEXT_FLAG);
            }

            if (position == 0) {
                itemView.setBackgroundResource(R.drawable.background_borders_bottom);
            } else if (position == getCount() - 1) {
                itemView.setBackgroundResource(R.drawable.background_borders_top);
            } else {
                itemView.setBackgroundResource(R.drawable.background_borders_top_bottom);
            }
        }
    }

    //endregion
}
