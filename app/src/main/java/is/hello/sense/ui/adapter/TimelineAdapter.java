package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineBarDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;
import rx.functions.Func1;

public class TimelineAdapter extends ArrayAdapter<TimelineSegment> {
    private static final int TYPE_BAR = 0;
    private static final int TYPE_EVENT = 1;
    private static final int TYPE_EVENT_WITH_ADJUST_TIME = 2;
    private static final int TYPE_EVENT_WITH_AUDIO = 3;
    private static final int TYPE_COUNT = 4;

    private final LayoutInflater inflater;
    private final Resources resources;

    protected final DateFormatter dateFormatter;
    protected boolean use24Time = false;

    private final int baseItemHeight;
    private final int itemEventImageHeight;

    private int[] itemHeights;

    private final Set<Integer> repeatedEventPositions = new HashSet<>();

    public TimelineAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, R.layout.item_simple_text);

        this.dateFormatter = dateFormatter;

        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();

        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.baseItemHeight = Math.max(minItemHeight, size.y / Styles.TIMELINE_HOURS_ON_SCREEN);
        this.itemEventImageHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_image_height);
    }


    //region Item Info

    protected int calculateItemHeight(@NonNull TimelineSegment segment) {
        if (segment.hasEventInfo()) {
            return (int) (Math.ceil(segment.getDuration() / 3600f) * this.itemEventImageHeight);
        } else {
            return (int) ((segment.getDuration() / 3600f) * this.baseItemHeight);
        }
    }

    protected void buildItemInfoCache(@NonNull List<TimelineSegment> segments) {
        this.itemHeights = new int[segments.size()];

        TimelineSegment lastEventSegment = null;
        for (int i = 0, size = itemHeights.length; i < size; i++) {
            TimelineSegment segment = segments.get(i);
            int height = calculateItemHeight(segment);
            this.itemHeights[i] = height;

            if (segment.hasEventInfo()) {
                if (lastEventSegment != null &&
                        lastEventSegment.getEventType() == segment.getEventType() &&
                        TextUtils.equals(lastEventSegment.getMessage(), segment.getMessage())) {
                    repeatedEventPositions.add(i);
                }

                lastEventSegment = segment;
            }
        }
    }

    @Override
    public void clear() {
        super.clear();

        repeatedEventPositions.clear();
        this.itemHeights = null;
    }

    //endregion


    //region Binding

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;
        notifyDataSetChanged();
    }

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            buildItemInfoCache(segments);
            addAll(segments);
        }
    }

    @SuppressWarnings("UnusedParameters")
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
        TimelineSegment segment = getItem(position);
        if (segment.hasEventInfo()) {
            if (segment.isTimeAdjustable()) {
                return TYPE_EVENT_WITH_ADJUST_TIME;
            } else if (segment.getSound() != null) {
                return TYPE_EVENT_WITH_AUDIO;
            } else {
                return TYPE_EVENT;
            }
        } else {
            return TYPE_BAR;
        }
    }

    private View inflateEventView(@LayoutRes int layoutRes,
                                  @NonNull ViewGroup parent,
                                  @NonNull Func1<View, ? extends ViewHolder> viewHolderFactory) {
        View view = inflater.inflate(layoutRes, parent, false);
        view.setTag(viewHolderFactory.call(view));
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimelineSegment segment = getItem(position);
        View view = convertView;
        if (view == null) {
            if (segment.hasEventInfo()) {
                if (segment.isTimeAdjustable()) {
                    view = inflateEventView(R.layout.item_timeline_event_actionable, parent, ActionableEventViewHolder::new);
                } else if (segment.getSound() != null) {
                    view = inflateEventView(R.layout.item_timeline_event, parent, EventViewHolder::new);
                } else {
                    view = inflateEventView(R.layout.item_timeline_event, parent, EventViewHolder::new);
                }
            } else {
                view = new View(getContext());
                view.setTag(new BarViewHolder(view));
            }
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.bind(position, segment);

        return view;
    }

    static class ViewHolder {
        final View itemView;

        ViewHolder(@NonNull View itemView) {
            this.itemView = itemView;
        }

        void bind(int position, @NonNull TimelineSegment segment) {
        }
    }

    class BarViewHolder extends ViewHolder {
        final TimelineBarDrawable bar;

        BarViewHolder(@NonNull View itemView) {
            super(itemView);

            this.bar = new TimelineBarDrawable(resources);
            itemView.setBackground(bar);
        }

        void bind(int position, @NonNull TimelineSegment segment) {
            bar.bind(segment);
            itemView.setMinimumHeight(itemHeights[position]);
        }
    }

    class EventViewHolder extends ViewHolder {
        final ImageView image;
        final TextView text;
        final TextView date;

        EventViewHolder(@NonNull View view) {
            super(view);

            this.image = (ImageView) view.findViewById(R.id.item_timeline_event_image);
            this.text = (TextView) view.findViewById(R.id.item_timeline_event_text);
            this.date = (TextView) view.findViewById(R.id.item_timeline_event_date);
        }

        TimelineBarDrawable getBarDrawable() {
            Drawable background = itemView.getBackground();
            if (background instanceof TimelineBarDrawable) {
                return (TimelineBarDrawable) background;
            } else {
                return new TimelineBarDrawable(resources);
            }
        }

        void bind(int position, @NonNull TimelineSegment segment) {
            image.setImageResource(Styles.getTimelineEventIconRes(segment));
            image.setContentDescription(resources.getString(segment.getEventType().nameString));

            date.setText(dateFormatter.formatAsTime(segment.getShiftedTimestamp(), use24Time));
            if (segment.isTimeAdjustable()) {
                date.setTextAppearance(getContext(), R.style.AppTheme_Text_TimelineDate_Actionable);
                date.setPaintFlags(date.getPaintFlags() | TextPaint.UNDERLINE_TEXT_FLAG);
            } else {
                date.setTextAppearance(getContext(), R.style.AppTheme_Text_TimelineDate);
                date.setPaintFlags(date.getPaintFlags() & ~TextPaint.UNDERLINE_TEXT_FLAG);
            }

            if (repeatedEventPositions.contains(position)) {
                TimelineBarDrawable barDrawable = getBarDrawable();
                barDrawable.bind(segment);
                itemView.setBackground(barDrawable);

                image.setScaleX(0.8f);
                image.setScaleY(0.8f);

                text.setText(null);
            } else {
                if (position == 0) {
                    itemView.setBackgroundResource(R.drawable.background_borders_bottom);
                } else if (position == getCount() - 1) {
                    itemView.setBackgroundResource(R.drawable.background_borders_top);
                } else {
                    itemView.setBackgroundResource(R.drawable.background_borders_top_bottom);
                }

                image.setScaleX(1f);
                image.setScaleY(1f);

                text.setText(segment.getMessage());
            }
        }
    }

    class ActionableEventViewHolder extends EventViewHolder {
        final Button action;

        ActionableEventViewHolder(@NonNull View view) {
            super(view);

            this.action = (Button) view.findViewById(R.id.item_timeline_event_action);
            action.setClickable(false);
        }
    }

    //endregion
}
