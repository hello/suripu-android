package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.util.ColorUtils;
import is.hello.sense.util.DateFormatter;

public final class SegmentView extends FrameLayout {
    private HorizontalGraphView graphView;
    private ImageView eventTypeImage;
    private TextView eventType;
    private TextView message;
    private TextView time;

    @Inject DateFormatter dateFormatter;

    public SegmentView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public SegmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public SegmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }


    //region Displaying Data

    public void displaySegment(@NonNull TimelineSegment segment) {
        graphView.showSleepScore(segment.getSleepDepth());

        if (segment.getEventType() != null) {
            eventType.setText(segment.getEventType());
            message.setText(segment.getMessage());
            time.setText(dateFormatter.formatAsTime(new DateTime(segment.getTimestamp())));

            eventType.setVisibility(VISIBLE);
            eventTypeImage.setVisibility(VISIBLE);
            message.setVisibility(VISIBLE);
        } else {
            eventType.setVisibility(GONE);
            eventTypeImage.setVisibility(GONE);
            message.setVisibility(GONE);
        }
    }

    //endregion


    protected void initialize(@Nullable AttributeSet attributes, int defStyleAttr) {
        SenseApplication.getInstance().inject(this);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_timeline_segment, this, true);

        this.graphView = (HorizontalGraphView) findViewById(R.id.view_timeline_segment_graph);
        this.eventTypeImage = (ImageView) findViewById(R.id.view_timeline_segment_image_event_type);
        this.eventType = (TextView) findViewById(R.id.view_timeline_segment_event_type);
        this.message = (TextView) findViewById(R.id.view_timeline_segment_message);
        this.time = (TextView) findViewById(R.id.view_timeline_segment_time);
    }
}
