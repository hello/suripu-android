package is.hello.sense.ui.widget.timeline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;

import is.hello.sense.R;
import is.hello.sense.ui.widget.TextDrawable;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineSegmentDrawable extends Drawable {
    private final Resources resources;
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

    private final int rightInset;
    private final int dividerHeight;
    private final int stolenScoreHeight;

    private final Drawable overlayDrawable;
    private final Rect fillRect = new Rect();

    private final TextDrawable timestampDrawable;

    private int sleepDepthColor;
    private float sleepDepthFraction;

    private int stolenTopSleepDepthColor;
    private float stolenTopSleepDepthFraction;

    private int stolenBottomSleepDepthColor;
    private float stolenBottomSleepDepthFraction;

    //region Lifecycle

    public TimelineSegmentDrawable(@NonNull Context context) {
        this.resources = context.getResources();
        this.rightInset = resources.getDimensionPixelSize(R.dimen.timeline_segment_item_end_inset);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.bottom_line);

        this.stolenScoreHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_stolen_height);

        this.timestampDrawable = new TextDrawable(context, R.style.AppTheme_Text_Timeline_Timestamp);
        this.overlayDrawable = ResourcesCompat.getDrawable(resources, R.drawable.timeline_segment_shading, null);

        stripePaint.setColor(resources.getColor(R.color.timeline_segment_stripe));

        setSleepDepth(0);
    }

    @Override
    public void draw(Canvas canvas) {
        int canvasRight = canvas.getWidth(),
            canvasBottom = canvas.getHeight();

        int contentRight = (canvasRight - rightInset);

        //region Sleep depths

        boolean hasStolenTopSleepDepth = (stolenBottomSleepDepthFraction > 0f);
        boolean hasStolenBottomSleepDepth = (stolenBottomSleepDepthFraction > 0f);

        if (sleepDepthFraction > 0f) {
            fillPaint.setColor(sleepDepthColor);

            fillRect.right = Math.round(contentRight * sleepDepthFraction);
            fillRect.top = hasStolenTopSleepDepth ? stolenScoreHeight : 0;
            fillRect.bottom = hasStolenBottomSleepDepth ? canvasBottom - stolenScoreHeight : canvasBottom;

            canvas.drawRect(fillRect, fillPaint);
            overlayDrawable.setBounds(fillRect);
            overlayDrawable.draw(canvas);
        }

        if (hasStolenTopSleepDepth) {
            fillPaint.setColor(stolenTopSleepDepthColor);

            fillRect.right = Math.round(contentRight * stolenTopSleepDepthFraction);
            fillRect.top = 0;
            fillRect.bottom = stolenScoreHeight;

            canvas.drawRect(fillRect, fillPaint);
            overlayDrawable.setBounds(fillRect);
            overlayDrawable.draw(canvas);
        }

        if (hasStolenBottomSleepDepth) {
            fillPaint.setColor(stolenBottomSleepDepthColor);

            fillRect.right = Math.round(contentRight * stolenBottomSleepDepthFraction);
            fillRect.top = canvasBottom - stolenScoreHeight;
            fillRect.bottom = canvasBottom;

            canvas.drawRect(fillRect, fillPaint);
            overlayDrawable.setBounds(fillRect);
            overlayDrawable.draw(canvas);
        }

        //endregion


        //region Time stamps

        if (timestampDrawable.getText() != null) {
            float middle = canvasBottom / 2f;
            float halfDividerHeight = dividerHeight / 2f;
            canvas.drawRect(0f, middle - halfDividerHeight,
                            contentRight, middle + halfDividerHeight,
                            stripePaint);

            int textWidthHalf = timestampDrawable.getIntrinsicWidth() / 2,
                textHeightHalf = timestampDrawable.getIntrinsicHeight() / 2;

            int midX = (canvasRight + contentRight) / 2;
            int midY = canvasBottom / 2;

            timestampDrawable.setBounds(
                midX - textWidthHalf, midY - textHeightHalf,
                midX + textWidthHalf, midY + textHeightHalf
            );
            timestampDrawable.draw(canvas);
        }

        //endregion
    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.right += rightInset;

        return true;
    }

    //endregion


    //region Drawing Attributes

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fillPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    //endregion


    //region Sleep Depths

    private static float calculateSleepDepthFraction(int sleepDepth) {
        float fraction = Math.min(1f, sleepDepth / 100f);
        if (sleepDepth <= 33) {
            return Math.max(0.10f, fraction);
        } else if (sleepDepth <= 66) {
            return Math.max(0.4f, fraction);
        } else {
            return fraction;
        }
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepthFraction = calculateSleepDepthFraction(sleepDepth);
        this.sleepDepthColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));

        invalidateSelf();
    }

    public void setStolenTopSleepDepth(int sleepDepth) {
        this.stolenTopSleepDepthFraction = calculateSleepDepthFraction(sleepDepth);
        this.stolenTopSleepDepthColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));

        invalidateSelf();
    }

    public void setStolenBottomSleepDepth(int sleepDepth) {
        this.stolenBottomSleepDepthFraction = calculateSleepDepthFraction(sleepDepth);
        this.stolenBottomSleepDepthColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));

        invalidateSelf();
    }

    //endregion


    //region Timestamps

    public void setTimestamp(@Nullable CharSequence timestamp) {
        timestampDrawable.setText(timestamp);
        invalidateSelf();
    }

    //endregion
}
