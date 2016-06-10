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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.TypedValue;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.ui.widget.TextDrawable;

public class TimelineSegmentDrawable extends Drawable {
    private final Resources resources;
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();
    private final double maxContentWidthFraction;
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

    public TimelineSegmentDrawable(@NonNull final Context context) {
        this.resources = context.getResources();
        final TypedValue typedValue = new TypedValue();
        resources.getValue(R.dimen.timeline_segment_item_end_max_fraction, typedValue, true);
        this.maxContentWidthFraction = typedValue.getFloat();
        this.rightInset = resources.getDimensionPixelSize(R.dimen.timeline_segment_item_end_inset);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.bottom_line);

        this.stolenScoreHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_stolen_height);

        this.timestampDrawable = new TextDrawable(context, R.style.AppTheme_Text_Timeline_Timestamp);
        this.overlayDrawable = ResourcesCompat.getDrawable(resources, R.drawable.timeline_segment_shading, null);

        stripePaint.setColor(ContextCompat.getColor(context, R.color.timeline_segment_stripe));

        setSleepDepth(0, TimelineEvent.SleepState.AWAKE);
    }

    @Override
    public void draw(Canvas canvas) {
        final int canvasRight = canvas.getWidth(),
            canvasBottom = canvas.getHeight();

        final long contentRight = Math.round(canvasRight * maxContentWidthFraction);

        final int timestampRight = canvasRight - rightInset;

        //region Sleep depths

        boolean hasStolenTopSleepDepth = (stolenBottomSleepDepthFraction > 0f);
        boolean hasStolenBottomSleepDepth = (stolenBottomSleepDepthFraction > 0f);

        //region Time stamps

        if (timestampDrawable.getText() != null) {
            final float middle = canvasBottom / 2f;
            final float halfDividerHeight = dividerHeight / 2f;
            canvas.drawRect(0f, middle - halfDividerHeight,
                            timestampRight, middle + halfDividerHeight,
                            stripePaint);

            final int textWidthHalf = timestampDrawable.getIntrinsicWidth() / 2,
                    textHeightHalf = timestampDrawable.getIntrinsicHeight() / 2;

            final int midX = (canvasRight + timestampRight) / 2;
            final int midY = canvasBottom / 2;

            timestampDrawable.setBounds(
                    midX - textWidthHalf, midY - textHeightHalf,
                    midX + textWidthHalf, midY + textHeightHalf
                                       );
            timestampDrawable.draw(canvas);
        }

        //endregion

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

    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
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
        return Math.min(1f, sleepDepth / 100f);
    }

    public void setSleepDepth(int sleepDepth, @NonNull TimelineEvent.SleepState sleepState) {
        this.sleepDepthFraction = calculateSleepDepthFraction(sleepDepth);
        this.sleepDepthColor = resources.getColor(sleepState.colorRes);

        invalidateSelf();
    }

    public float getSleepDepthFraction() {
        return sleepDepthFraction;
    }

    public int getSleepDepthColor() {
        return sleepDepthColor;
    }

    public void setStolenTopSleepDepth(int sleepDepth, @NonNull TimelineEvent.SleepState sleepState) {
        this.stolenTopSleepDepthFraction = calculateSleepDepthFraction(sleepDepth);
        this.stolenTopSleepDepthColor = resources.getColor(sleepState.colorRes);

        invalidateSelf();
    }

    public float getStolenTopSleepDepthFraction() {
        return stolenTopSleepDepthFraction;
    }

    public int getStolenTopSleepDepthColor() {
        return stolenTopSleepDepthColor;
    }

    public void setStolenBottomSleepDepth(int sleepDepth, @NonNull TimelineEvent.SleepState sleepState) {
        this.stolenBottomSleepDepthFraction = calculateSleepDepthFraction(sleepDepth);
        this.stolenBottomSleepDepthColor = resources.getColor(sleepState.colorRes);

        invalidateSelf();
    }

    public float getStolenBottomSleepDepthFraction() {
        return stolenBottomSleepDepthFraction;
    }

    public int getStolenBottomSleepDepthColor() {
        return stolenBottomSleepDepthColor;
    }

    //endregion


    //region Timestamps

    public void setTimestamp(@Nullable CharSequence timestamp) {
        timestampDrawable.setText(timestamp);
        invalidateSelf();
    }

    public @Nullable CharSequence getTimestamp() {
        return timestampDrawable.getText();
    }

    //endregion
}
