package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.util.DateFormatter;

@SuppressWarnings("UnusedDeclaration")
public final class TimelineSegmentView extends FrameLayout {
    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private HorizontalBarGraphView graphView;
    private View stripe;
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

    public void displaySegment(@NonNull TimelineSegment segment) {
        int sleepDepth = segment.getSleepDepth();
        graphView.setFillColor(getResources().getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));
        graphView.setValue(sleepDepth);

        stripe.setBackgroundResource(Styles.getSleepDepthColorRes(sleepDepth));
        time.setText(dateFormatter.formatAsTime(segment.getTimestamp()));

        if (segment.getEventType() != null) {
            eventTypeImage.setImageResource(segment.getEventType().iconDrawable);
            eventType.setText(segment.getEventType().nameString);
            time.setBackgroundResource(R.drawable.timestamp_background);
            int horizontalPadding = (int) (10f * displayMetrics.density);
            int verticalPadding = (int) (6f * displayMetrics.density);
            time.setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            time.setTextAppearance(getContext(), R.style.AppTheme_Text_Body);
            time.setVisibility(VISIBLE);

            eventTypeImage.setVisibility(VISIBLE);
            eventType.setVisibility(VISIBLE);
        } else {
            eventTypeImage.setImageDrawable(null);

            time.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Dimmed);
            time.setBackground(null);
            time.setPaddingRelative(0, 0, 0, 0);
            time.setVisibility(segment.getTimestamp().getMinuteOfHour() == 0 ? VISIBLE : GONE);

            eventTypeImage.setVisibility(INVISIBLE);
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
        this.stripe = findViewById(R.id.view_timeline_segment_event_stripe);
        this.eventTypeImage = (ImageView) findViewById(R.id.view_timeline_segment_image_event_type);
        this.eventType = (TextView) findViewById(R.id.view_timeline_segment_event_type);
        this.time = (TextView) findViewById(R.id.view_timeline_segment_time);
    }
}
