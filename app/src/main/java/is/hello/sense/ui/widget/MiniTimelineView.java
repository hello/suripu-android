package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.widget.util.Styles;

public class MiniTimelineView extends View {
    private final Paint fillPaint = new Paint();
    private final Paint linePaint = new Paint();
    private final int stripeWidth;
    private final boolean useModernDesign;

    private @Nullable List<TimelineSegment> timelineSegments;


    public MiniTimelineView(@NonNull Context context) {
        this(context, null);
    }

    public MiniTimelineView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniTimelineView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.stripeWidth = getResources().getDimensionPixelSize(R.dimen.view_mini_timeline_stripe_width);
        linePaint.setColor(getResources().getColor(R.color.timeline_segment_stripe));

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.useModernDesign = preferences.getBoolean(PreferencesPresenter.USE_MODERN_TIMELINE, true);
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

                int dimmedColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth, segment.isBeforeSleep()));
                fillPaint.setColor(dimmedColor);

                if (useModernDesign) {
                    canvas.drawRect(0, y, segmentWidth, y + segmentHeight, fillPaint);
                } else {
                    canvas.drawRect(midX - segmentMidX, y, midX + segmentMidX, y + segmentHeight, fillPaint);
                }

                y += segmentHeight;
            }
        }

        if (!useModernDesign) {
            canvas.drawRect(midX - (stripeWidth / 2f), 0f, midX + (stripeWidth / 2f), height, linePaint);
        }
    }


    public void setTimelineSegments(@Nullable List<TimelineSegment> timelineSegments) {
        this.timelineSegments = timelineSegments;
        invalidate();
    }
}
