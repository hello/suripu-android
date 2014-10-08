package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.WindowManager;
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

@SuppressWarnings("UnusedDeclaration")
public final class TimelineSegmentView extends FrameLayout {
    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private HorizontalGraphView graphView;
    private ImageView eventTypeImage;
    private TextView eventType;
    private TextView time;

    @Inject DateFormatter dateFormatter;

    public TimelineSegmentView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public TimelineSegmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public TimelineSegmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }


    //region Displaying Data

    public String getNameForEventType(@NonNull TimelineSegment.EventType eventType) {
        switch (eventType) {
            case LIGHT:
                return getContext().getString(R.string.event_type_light);

            case MOTION:
                return getContext().getString(R.string.event_type_motion);

            case NOISE:
                return getContext().getString(R.string.event_type_noise);

            case SLEEP_MOTION:
                return getContext().getString(R.string.event_type_sleep_motion);

            case SLEEP_TALK:
                return getContext().getString(R.string.event_type_sleep_talk);

            case SNORING:
                return getContext().getString(R.string.event_type_snoring);

            case SLEEP:
                return getContext().getString(R.string.event_type_sleep);

            default:
                return getContext().getString(R.string.missing_data_placeholder);
        }
    }

    public void displaySegment(@NonNull TimelineSegment segment) {
        int sleepScore = segment.getSleepDepth();
        graphView.setFillColor(getResources().getColor(ColorUtils.colorResForSleepDepth(sleepScore)));
        graphView.setValue(sleepScore);

        time.setText(dateFormatter.formatAsTime(new DateTime(segment.getTimestamp())));

        if (segment.getEventType() != null) {
            eventType.setText(getNameForEventType(segment.getEventType()));
            time.setBackgroundResource(R.drawable.background_timestamp_highlighted);
            time.setTextColor(getResources().getColor(R.color.black));
            int padding = (int) (8f * displayMetrics.density);
            time.setPaddingRelative(padding, padding, padding, padding);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);

            eventType.setVisibility(VISIBLE);
            eventTypeImage.setVisibility(VISIBLE);
        } else {
            time.setBackground(null);
            time.setTextColor(getResources().getColor(R.color.grey));
            time.setPaddingRelative(0, 0, 0, 0);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);

            eventType.setVisibility(GONE);
            eventTypeImage.setVisibility(GONE);
        }
    }

    //endregion

    protected void initialize(@Nullable AttributeSet attributes, int defStyleAttr) {
        SenseApplication.getInstance().inject(this);

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(this.displayMetrics);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_timeline_segment, this, true);

        this.graphView = (HorizontalGraphView) findViewById(R.id.view_timeline_segment_graph);
        this.eventTypeImage = (ImageView) findViewById(R.id.view_timeline_segment_image_event_type);
        this.eventType = (TextView) findViewById(R.id.view_timeline_segment_event_type);
        this.time = (TextView) findViewById(R.id.view_timeline_segment_time);
    }
}
