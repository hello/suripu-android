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
import is.hello.sense.api.model.v2.TimelineEvent;

public class TimelinePreviewView extends View {
    private final Paint fillPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final int borderWidth;

    private @Nullable List<TimelineEvent> timelineEvents;


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

        if (timelineEvents != null && !timelineEvents.isEmpty()) {
            Resources resources = getResources();

            float segmentHeight = height / timelineEvents.size();
            float y = 0f;

            for (TimelineEvent event : timelineEvents) {
                int sleepDepth = event.getSleepDepth();
                float segmentWidth = width * (sleepDepth / 100f);

                int color = resources.getColor(event.getSleepState().colorRes);
                fillPaint.setColor(color);

                canvas.drawRect(0, y, segmentWidth, y + segmentHeight, fillPaint);

                y += segmentHeight;
            }
        }

        canvas.drawRect(0, 0, borderWidth, height, borderPaint);
    }


    public void setTimelineEvents(@Nullable List<TimelineEvent> events) {
        this.timelineEvents = events;
        invalidate();
    }
}
