package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
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

    protected AbstractTimelineAdapter(@NonNull Context context, @NonNull DateFormatter dateFormatter) {
        super(context, R.layout.item_simple_text);

        this.dateFormatter = dateFormatter;
    }


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
}
