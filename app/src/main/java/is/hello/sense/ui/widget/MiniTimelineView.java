package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;

public class MiniTimelineView extends View {
    private final Paint linePaint = new Paint();
    private final int stripeWidth;

    private @Nullable List<TimelineSegment> timelineSegments;


    public MiniTimelineView(Context context) {
        this(context, null);
    }

    public MiniTimelineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.stripeWidth = getResources().getDimensionPixelSize(R.dimen.view_mini_timeline_stripe_width);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Resources resources = getResources();

        if (timelineSegments != null && !timelineSegments.isEmpty()) {
            float segmentHeight = height / timelineSegments.size();
            float y = 0f;

            for (TimelineSegment segment : timelineSegments) {
                int sleepDepth = segment.getSleepDepth();
                float segmentWidth = width * (sleepDepth / 100f);

                int dimmedColor = resources.getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth));
                linePaint.setColor(dimmedColor);

                canvas.drawRect(0f, y, segmentWidth, y + segmentHeight, linePaint);

                int stripeColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));
                linePaint.setColor(stripeColor);

                canvas.drawRect(0f, y, stripeWidth, y + segmentHeight, linePaint);

                y += segmentHeight;
            }
        } else {
            int stripeColor = resources.getColor(R.color.sleep_awake_dimmed);
            linePaint.setColor(stripeColor);

            canvas.drawRect(0f, 0f, stripeWidth, height, linePaint);
        }
    }


    public void setTimelineSegments(@Nullable List<TimelineSegment> timelineSegments) {
        this.timelineSegments = timelineSegments;
        invalidate();
    }
}
