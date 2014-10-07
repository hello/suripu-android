package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.TimelineSegmentView;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    private static final int NUMBER_HOURS_ON_SCREEN = 4;

    private final int itemHeight;

    public TimelineSegmentAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_list_item_1);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        this.itemHeight = metrics.heightPixels / NUMBER_HOURS_ON_SCREEN;

        SenseApplication.getInstance().inject(this);
    }

    public void bindSegments(@Nullable List<TimelineSegment> segments) {
        clear();

        if (segments != null) {
            addAll(segments);
        }
    }

    public void handleError(@NonNull Throwable error) {
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

        TimelineSegment segment = getItem(position);
        view.displaySegment(segment);
        view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, calculateHeight(segment)));

        return view;
    }
}
