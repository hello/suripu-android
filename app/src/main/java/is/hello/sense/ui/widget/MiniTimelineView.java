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
    private final Paint fillPaint = new Paint();
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
        linePaint.setColor(getResources().getColor(R.color.timeline_segment_stripe));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        float midX = width / 2f;
        Resources resources = getResources();

        if (timelineSegments != null && !timelineSegments.isEmpty()) {
            float segmentHeight = height / timelineSegments.size();
            float y = 0f;

            for (TimelineSegment segment : timelineSegments) {
                int sleepDepth = segment.getSleepDepth();
                float segmentWidth = width * (sleepDepth / 100f);
                float segmentMidX = segmentWidth / 2f;

                int dimmedColor = resources.getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth));
                fillPaint.setColor(dimmedColor);

                canvas.drawRect(midX - segmentMidX, y, midX + segmentMidX, y + segmentHeight, fillPaint);

                y += segmentHeight;
            }
        }

        canvas.drawRect(midX - (stripeWidth / 2f), 0f, midX + (stripeWidth / 2f), height, linePaint);
    }


    public void setTimelineSegments(@Nullable List<TimelineSegment> timelineSegments) {
        this.timelineSegments = timelineSegments;
        invalidate();
    }
}
