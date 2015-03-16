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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;

/**
 * Contains the methods specific to a timeline adapter.
 * <p/>
 * Should be rolled back into the TimelineSegmentAdapter whenever
 * the new timeline design is completely rolled out.
 */
public abstract class AbstractTimelineAdapter extends ArrayAdapter<TimelineSegment> {
    /**
     * The date formatter to use when rendering the timeline.
     */
    protected final DateFormatter dateFormatter;

    /**
     * Whether or not to use 24 hour time when rendering the timeline
     * <p/>
     * Automatically bound and updated by the containing fragment.
     */
    protected boolean use24Time = false;

    private final int baseItemHeight;
    private final int itemEventImageHeight;

    private final Set<Integer> timePositions = new HashSet<>();
    private float[] itemHeights;

    /**
     * The number of event items contained in the adapter.
     */
    protected int eventItemCount = 0;

    /**
     * The number of bar items contained in the adapter.
     */
    protected int barItemCount = 0;


    protected AbstractTimelineAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, R.layout.item_simple_text);

        this.dateFormatter = dateFormatter;

        Resources resources = context.getResources();

        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.baseItemHeight = Math.max(minItemHeight, size.y / Styles.TIMELINE_HOURS_ON_SCREEN);
        this.itemEventImageHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_image_height);
    }


    //region Item Info

    /**
     * Calculates the height required to display the item at a given position.
     */
    protected int calculateItemHeight(@NonNull TimelineSegment segment) {
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
    protected void buildItemInfoCache(@NonNull List<TimelineSegment> segments) {
        this.itemHeights = new float[segments.size()];
        this.eventItemCount = 0;
        this.barItemCount = 0;

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

            if (segment.hasEventInfo()) {
                eventItemCount++;
            } else {
                barItemCount++;
            }
        }
    }

    @Override
    public void clear() {
        super.clear();

        this.eventItemCount = 0;
        this.barItemCount = 0;
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

    /**
     * Returns whether or not a given position should contain the time.
     */
    protected boolean isTimePosition(int position) {
        return timePositions.contains(position);
    }

    //endregion


    //region Bindings

    /**
     * Binding points for updating the use 24 hour time property.
     * <p/>
     * @see is.hello.sense.ui.fragments.TimelineFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;
        notifyDataSetChanged();
    }

    /**
     * Binding point for potential timeline segments.
     * <p/>
     * The implementation of this method should modify itself and notify its observers.
     */
    public abstract void bindSegments(@Nullable List<TimelineSegment> segments);

    /**
     * Binding point for potential errors.
     * <p/>
     * The implementation of this method should clear itself,
     * and <em>should not</em> present the given error.
     */
    public abstract void handleError(@NonNull Throwable e);

    /**
     * Subclass implementations of this method <em>must not</em> call <code>super</code>.
     */
    public abstract View getView(int position, View convertView, ViewGroup parent);

    //endregion
}
