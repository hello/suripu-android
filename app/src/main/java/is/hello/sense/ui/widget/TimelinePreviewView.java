package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;

public class TimelinePreviewView extends View {
    private final Paint fillPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final int borderWidth;

    private @Nullable List<TimelineSegment> timelineSegments;


    public TimelinePreviewView(@NonNull Context context) {
        this(context, null);
    }

    public TimelinePreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelinePreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources resources = getResources();

        int borderColor = resources.getColor(R.color.timeline_header_border);
        borderPaint.setColor(borderColor);

        this.borderWidth = resources.getDimensionPixelSize(R.dimen.divider_size);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (timelineSegments != null && !timelineSegments.isEmpty()) {
            Resources resources = getResources();

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

        canvas.drawRect(0, 0, borderWidth, height, borderPaint);
    }


    public void setTimelineSegments(@Nullable List<TimelineSegment> timelineSegments) {
        this.timelineSegments = timelineSegments;
        invalidate();
    }
}
