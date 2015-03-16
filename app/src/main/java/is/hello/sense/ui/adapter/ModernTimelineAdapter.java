package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.util.DateFormatter;

public class ModernTimelineAdapter extends AbstractTimelineAdapter {
    private static final int TYPE_BAR = 0;
    private static final int TYPE_EVENT = 1;
    private static final int TYPE_COUNT = 2;

    public ModernTimelineAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, dateFormatter);
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
                view = new View(getContext());
                view.setTag(new EventViewHolder(view));
            }

            EventViewHolder holder = (EventViewHolder) view.getTag();
            holder.bind(segment);
        } else {
            if (view == null) {
                view = new View(getContext());
                view.setTag(new BarViewHolder(view));
            }

            BarViewHolder holder = (BarViewHolder) view.getTag();
            holder.bind(segment);
            view.setMinimumHeight((int) getItemHeight(position));
        }
        return view;
    }

    class BarViewHolder {
        BarViewHolder(@NonNull View view) {

        }

        void bind(@NonNull TimelineSegment segment) {

        }
    }

    class EventViewHolder {
        EventViewHolder(@NonNull View view) {

        }

        void bind(@NonNull TimelineSegment segment) {

        }
    }

    //endregion
}
