package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.SegmentView;

public class TimelineSegmentAdapter extends ArrayAdapter<TimelineSegment> {
    public TimelineSegmentAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_list_item_1);

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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SegmentView view = (SegmentView) convertView;
        if (view == null) {
            view = new SegmentView(getContext());
        }

        TimelineSegment segment = getItem(position);
        view.displaySegment(segment);

        return view;
    }
}
