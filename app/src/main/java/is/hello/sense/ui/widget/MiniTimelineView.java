package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;

public class MiniTimelineView extends View {
    private final Paint fillPaint = new Paint();

    private @Nullable List<TimelineSegment> timelineSegments;


    public MiniTimelineView(Context context) {
        this(context, null);
    }

    public MiniTimelineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

                int dimmedColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));
                fillPaint.setColor(dimmedColor);

                canvas.drawRect(0, y, segmentWidth, y + segmentHeight, fillPaint);

                y += segmentHeight;
            }
        }
    }


    public void setTimelineSegments(@Nullable List<TimelineSegment> timelineSegments) {
        this.timelineSegments = timelineSegments;
        invalidate();
    }
}
