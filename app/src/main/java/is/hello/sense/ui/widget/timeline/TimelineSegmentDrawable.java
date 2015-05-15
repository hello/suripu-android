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

import is.hello.sense.R;
import is.hello.sense.ui.widget.TextDrawable;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineSegmentDrawable extends Drawable implements Drawable.Callback {
    private final Resources resources;
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

    private final int rightInset;
    private final int dividerHeight;
    private final int stolenScoreHeight;

    private final TextDrawable timestampDrawable;

    private @Nullable Drawable childDrawable;
    private final Rect childDrawablePadding = new Rect();

    private int sleepDepthColor;
    private float sleepDepthFraction;

    private int stolenTopSleepDepthColor;
    private float stolenTopSleepDepthFraction;

    private int stolenBottomSleepDepthColor;
    private float stolenBottomSleepDepthFraction;

    //region Lifecycle

    public TimelineSegmentDrawable(@NonNull Context context) {
        this.resources = context.getResources();
        this.rightInset = resources.getDimensionPixelSize(R.dimen.timeline_segment_item_right_inset);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
        this.stolenScoreHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_stolen_height);

        this.timestampDrawable = new TextDrawable(context, R.style.AppTheme_Text_Timeline_Timestamp);

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

            float fillRight = contentRight * sleepDepthFraction;
            float fillTop = hasStolenTopSleepDepth ? stolenScoreHeight : 0f;
            float fillBottom = hasStolenBottomSleepDepth ? canvasBottom - stolenScoreHeight : canvasBottom;
            canvas.drawRect(0f, fillTop, fillRight, fillBottom, fillPaint);
        }

        if (hasStolenTopSleepDepth) {
            fillPaint.setColor(stolenTopSleepDepthColor);

            float fillRight = contentRight * stolenTopSleepDepthFraction;
            float fillBottom = stolenScoreHeight;
            canvas.drawRect(0f, 0f, fillRight, fillBottom, fillPaint);
        }

        if (hasStolenBottomSleepDepth) {
            fillPaint.setColor(stolenBottomSleepDepthColor);

            float fillRight = contentRight * stolenBottomSleepDepthFraction;
            float fillTop = canvasBottom - stolenScoreHeight;
            canvas.drawRect(0f, fillTop, fillRight, canvasBottom, fillPaint);
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


        if (childDrawable != null) {
            childDrawable.setBounds(
                childDrawablePadding.left, childDrawablePadding.top,
                contentRight - childDrawablePadding.right, canvasBottom - childDrawablePadding.bottom
            );
            childDrawable.draw(canvas);
        }
    }

    //endregion


    //region Drawing Attributes

    @Override
    public void setAlpha(int alpha) {
        if (childDrawable != null) {
            childDrawable.setAlpha(alpha);
        }
        fillPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (childDrawable != null) {
            childDrawable.setColorFilter(cf);
        }
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

    //region Child Drawables

    public void setChildDrawable(@Nullable Drawable childDrawable) {
        if (this.childDrawable != null) {
            this.childDrawable.setCallback(null);
        }

        this.childDrawable = childDrawable;

        if (childDrawable != null) {
            childDrawable.setCallback(this);

            childDrawable.setAlpha(fillPaint.getAlpha());
            childDrawable.setColorFilter(fillPaint.getColorFilter());
            childDrawable.setState(getState());
        }

        invalidateSelf();
    }

    public void setChildDrawablePadding(int left, int top, int right, int bottom) {
        childDrawablePadding.set(left, top, right, bottom);

        invalidateSelf();
    }

    //endregion


    //region Timestamps

    public void setTimestamp(@Nullable CharSequence timestamp) {
        timestampDrawable.setText(timestamp);
        invalidateSelf();
    }

    //endregion


    //region Forwarding

    @Override
    public boolean getPadding(Rect padding) {
        if (childDrawable != null) {
            childDrawable.getPadding(padding);
        }

        padding.left += childDrawablePadding.left;
        padding.top += childDrawablePadding.top;
        padding.right += childDrawablePadding.right + rightInset;
        padding.bottom += childDrawablePadding.bottom;

        return true;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return (childDrawable != null && childDrawable.setState(state));
    }

    @Override
    public boolean isStateful() {
        return (childDrawable != null && childDrawable.isStateful());
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    //endregion
}
