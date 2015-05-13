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

public class TimelineSegmentDrawable extends Drawable {
    private final Resources resources;
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

    private final Drawable backgroundFill;
    private final int rightInset;
    private final int dividerHeight;
    private final int stolenScoreHeight;

    private final TextDrawable timestampDrawable;

    private @Nullable Drawable overlayDrawable;

    private int sleepDepthColor;
    private float sleepDepthFraction;

    private int stolenTopSleepDepthColor;
    private float stolenTopSleepDepthFraction;

    private int stolenBottomSleepDepthColor;
    private float stolenBottomSleepDepthFraction;

    public TimelineSegmentDrawable(@NonNull Context context) {
        this.resources = context.getResources();
        this.backgroundFill = resources.getDrawable(R.drawable.background_timeline_segment2);
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

        backgroundFill.setBounds(0, 0, canvasRight, canvasBottom);
        backgroundFill.draw(canvas);

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


        if (overlayDrawable != null) {
            overlayDrawable.setBounds(0, 0, contentRight, canvasBottom);
            overlayDrawable.draw(canvas);
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (overlayDrawable != null) {
            overlayDrawable.getPadding(padding);
        }

        padding.right += rightInset;

        return true;
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
        this.sleepDepthColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));

        invalidateSelf();
    }

    public void setStolenTopSleepDepth(int sleepDepth) {
        this.stolenTopSleepDepthFraction = Math.min(1f, sleepDepth / 100f);
        this.stolenTopSleepDepthColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));

        invalidateSelf();
    }

    public void setStolenBottomSleepDepth(int sleepDepth) {
        this.stolenBottomSleepDepthFraction = Math.min(1f, sleepDepth / 100f);
        this.stolenBottomSleepDepthColor = resources.getColor(Styles.getSleepDepthColorRes(sleepDepth));

        invalidateSelf();
    }

    public void setOverlayDrawable(@Nullable Drawable overlayDrawable) {
        this.overlayDrawable = overlayDrawable;

        invalidateSelf();
    }

    public void setTimestamp(@Nullable CharSequence timestamp) {
        timestampDrawable.setText(timestamp);
        invalidateSelf();
    }

    //endregion
}
