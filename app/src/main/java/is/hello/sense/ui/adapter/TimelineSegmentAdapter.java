package is.hello.sense.ui.adapter;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import java.util.List;

import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineSegmentView;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    private static final int NUMBER_HOURS_ON_SCREEN = 4;

    private final int itemHeight;

    //region Lifecycle

    public TimelineSegmentAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_list_item_1);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        this.itemHeight = size.y / NUMBER_HOURS_ON_SCREEN;
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

    private int calculateHeight(@NonNull TimelineSegment segment) {
        return (int) (segment.getDuration() / 3600) * itemHeight;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TimelineSegmentView view = (TimelineSegmentView) convertView;
        if (view == null) {
            view = new TimelineSegmentView(getContext());
        }

        TimelineSegmentView.Position segmentPosition = TimelineSegmentView.Position.MIDDLE;
        if (position == 0)
            segmentPosition = TimelineSegmentView.Position.FIRST;
        else if (position == getCount() - 1)
            segmentPosition = TimelineSegmentView.Position.LAST;

        TimelineSegment segment = getItem(position);
        view.displaySegment(segment, segmentPosition);
        view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, calculateHeight(segment)));

        return view;
    }
}
