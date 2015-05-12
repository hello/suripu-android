package is.hello.sense.ui.widget.timeline;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineSegmentDrawable extends Drawable {
    private final Resources resources;
    private final Paint fillPaint = new Paint();
    private final Drawable backgroundFill;

    private float sleepDepthFraction;

    public TimelineSegmentDrawable(@NonNull Resources resources) {
        this.resources = resources;
        this.backgroundFill = resources.getDrawable(R.drawable.background_timeline_segment2);

        setSleepDepth(0);
    }


    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth(),
            height = canvas.getHeight();

        backgroundFill.setBounds(0, 0, width, height);
        backgroundFill.draw(canvas);

        if (sleepDepthFraction > 0) {
            float right = width * sleepDepthFraction;
            canvas.drawRect(0f, 0f, right, height, fillPaint);
        }
    }


    //region Attributes

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fillPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepthFraction = Math.min(1f, sleepDepth / 100f);

        int color = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth, false));
        fillPaint.setColor(color);

        invalidateSelf();
    }

    //endregion
}
