package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
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

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.common.Styles;

@SuppressWarnings("UnusedDeclaration")
public final class TimelineSegmentView extends FrameLayout {
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private int stripeCornerRadius;

    private HorizontalBarGraphView graphView;
    private View stripe;
    private ImageView eventTypeImage;
    private TextView eventType;

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

    public @NonNull Drawable createRoundedDrawable(int color, float[] radii) {
        RoundRectShape shape = new RoundRectShape(radii, null, null);
        ShapeDrawable drawable = new ShapeDrawable(shape);
        drawable.getPaint().setColor(color);
        return drawable;
    }

    public void displaySegment(@NonNull TimelineSegment segment, @NonNull Position position) {
        int sleepDepth = segment.getSleepDepth() < 0 ? 0 : segment.getSleepDepth();
        graphView.setFillColor(getResources().getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));
        graphView.setValue(sleepDepth);

        int colorRes = Styles.getSleepDepthColorRes(sleepDepth);
        if (position == Position.FIRST) {
            float[] radii = {
                    stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                    0f, 0f, 0f, 0f,
            };
            stripe.setBackground(createRoundedDrawable(getResources().getColor(colorRes), radii));
        } else if (position == Position.LAST) {
            float[] radii = {
                    0f, 0f, 0f, 0f,
                    stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
            };
            stripe.setBackground(createRoundedDrawable(getResources().getColor(colorRes), radii));
        } else {
            stripe.setBackgroundResource(colorRes);
        }

        if (segment.getEventType() != null) {
            eventTypeImage.setImageResource(segment.getEventType().iconRes);
            eventType.setText(segment.getEventType().nameString);

            eventTypeImage.setVisibility(VISIBLE);
            eventType.setVisibility(VISIBLE);
        } else {
            eventTypeImage.setImageDrawable(null);

            eventTypeImage.setVisibility(GONE);
            eventType.setVisibility(GONE);
        }
    }

    //endregion

    protected void initialize(@Nullable AttributeSet attributes, int defStyleAttr) {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(this.displayMetrics);

        this.stripeCornerRadius = getResources().getDimensionPixelOffset(R.dimen.timeline_stripe_corner_radius);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_timeline_segment, this, true);

        this.graphView = (HorizontalBarGraphView) findViewById(R.id.view_timeline_segment_graph);
        this.stripe = findViewById(R.id.view_timeline_segment_event_stripe);
        this.eventTypeImage = (ImageView) findViewById(R.id.view_timeline_segment_image_event_type);
        this.eventType = (TextView) findViewById(R.id.view_timeline_segment_event_type);
    }


    public static enum Position {
        FIRST,
        MIDDLE,
        LAST,
    }
}
