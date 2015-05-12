package is.hello.sense.ui.widget.timeline;

import android.content.Context;
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
    private final Paint stripePaint = new Paint();

    private final Drawable backgroundFill;
    private final int rightInset;
    private final int dividerHeight;

    private boolean wantsDivider;
    private float sleepDepthFraction;

    public TimelineSegmentDrawable(@NonNull Context context) {
        this.resources = context.getResources();
        this.backgroundFill = resources.getDrawable(R.drawable.background_timeline_segment2);
        this.rightInset = resources.getDimensionPixelSize(R.dimen.timeline_segment2_item_right_inset);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);

        stripePaint.setColor(resources.getColor(R.color.timeline_segment_stripe));

        setSleepDepth(0);
    }


    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth(),
            height = canvas.getHeight();

        backgroundFill.setBounds(0, 0, width, height);
        backgroundFill.draw(canvas);

        float right = (width - rightInset);

        if (sleepDepthFraction > 0) {
            float fillRight = right * sleepDepthFraction;
            canvas.drawRect(0f, 0f, fillRight, height, fillPaint);
        }

        if (wantsDivider) {
            float middle = height / 2f;
            float halfDividerHeight = dividerHeight / 2f;
            canvas.drawRect(0f, middle - halfDividerHeight,
                            right, middle + halfDividerHeight,
                            stripePaint);
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

    public void setWantsDivider(boolean wantsDivider) {
        this.wantsDivider = wantsDivider;
        invalidateSelf();
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepthFraction = Math.min(1f, sleepDepth / 100f);

        int color = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth, false));
        fillPaint.setColor(color);

        invalidateSelf();
    }

    //endregion
}
