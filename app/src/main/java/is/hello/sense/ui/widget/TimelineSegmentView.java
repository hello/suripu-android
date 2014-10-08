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

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.util.Styles;
import is.hello.sense.util.DateFormatter;

@SuppressWarnings("UnusedDeclaration")
public final class TimelineSegmentView extends FrameLayout {
    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private HorizontalBarGraphView graphView;
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
        int sleepDepth = segment.getSleepDepth();
        graphView.setFillColor(getResources().getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));
        graphView.setValue(sleepDepth);

        eventTypeImage.setBackgroundResource(Styles.getSleepDepthColorRes(sleepDepth));
        time.setText(dateFormatter.formatAsTime(segment.getTimestamp()));

        if (segment.getEventType() != null) {
            eventTypeImage.setImageResource(segment.getEventType().iconDrawable);
            eventType.setText(getNameForEventType(segment.getEventType()));
            time.setBackgroundResource(R.drawable.background_timestamp_highlighted);
            time.setTextColor(getResources().getColor(R.color.black));
            int horizontalPadding = (int) (8f * displayMetrics.density);
            int verticalPadding = (int) (4f * displayMetrics.density);
            time.setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            time.setVisibility(VISIBLE);

            eventType.setVisibility(VISIBLE);
        } else {
            eventTypeImage.setImageDrawable(null);

            time.setBackground(null);
            time.setTextColor(getResources().getColor(R.color.grey));
            time.setPaddingRelative(0, 0, 0, 0);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            time.setVisibility(segment.getTimestamp().getMinuteOfHour() == 0 ? VISIBLE : GONE);

            eventType.setVisibility(INVISIBLE);
        }
    }

    //endregion

    protected void initialize(@Nullable AttributeSet attributes, int defStyleAttr) {
        SenseApplication.getInstance().inject(this);

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(this.displayMetrics);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_timeline_segment, this, true);

        this.graphView = (HorizontalBarGraphView) findViewById(R.id.view_timeline_segment_graph);
        this.eventTypeImage = (ImageView) findViewById(R.id.view_timeline_segment_image_event_type);
        this.eventType = (TextView) findViewById(R.id.view_timeline_segment_event_type);
        this.time = (TextView) findViewById(R.id.view_timeline_segment_time);
    }
}
