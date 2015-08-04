package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawing;

public class SleepScoreDrawable extends Drawable {
    private static final float ANGLE_START = -225f;
    private static final float ANGLE_SWEEP = 270f;

    private static final float MAX_VALUE = 100;

    private final Resources resources;

    private final Path fillPath = new Path();
    private final Path arcPath = new Path();
    private final RectF arcRect = new RectF();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float fillStrokeWidth;
    private final int pressedColor;

    private final TextPaint labelPaint;
    private final String label;
    private final int labelHeight;

    private boolean stateful = false;
    private boolean pressed = false;

    private float value = 0f;
    private int alpha = 255;
    private int fillColor;
    private int trackColor;


    public SleepScoreDrawable(@NonNull Resources resources, boolean wantsLabel) {
        this.resources = resources;
        this.fillStrokeWidth = resources.getDimensionPixelSize(R.dimen.pie_graph_stroke_width);
        this.fillColor = Color.TRANSPARENT;
        this.trackColor = resources.getColor(R.color.border);
        this.pressedColor = resources.getColor(R.color.background_dark_item_clicked);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(fillStrokeWidth);

        if (wantsLabel) {
            this.labelPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | TextPaint.SUBPIXEL_TEXT_FLAG);
            labelPaint.setTextSize(resources.getDimension(R.dimen.text_size_section_heading));
            labelPaint.setColor(resources.getColor(R.color.text_section_header));
            labelPaint.setTextAlign(Paint.Align.CENTER);

            this.label = resources.getString(R.string.sleep_score).toUpperCase();

            int offset = resources.getDimensionPixelSize(R.dimen.gap_tiny);
            this.labelHeight = Drawing.getEstimatedTextHeight(labelPaint) + offset;
        } else {
            this.labelPaint = null;
            this.label = null;
            this.labelHeight = 0;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        arcPath.reset();
        fillPath.reset();

        int width = canvas.getWidth(),
            height = canvas.getHeight();
        int size = Math.min(width, height);

        float left = width > height ? (width - height) / 2 : 0;
        float top = width < height ? (height - width) / 2 : 0;

        arcRect.set(left, top, size + left, size + top);
        arcRect.inset(fillStrokeWidth / 2f, fillStrokeWidth / 2f);

        float scale = (value / MAX_VALUE);
        if (scale > 0f) {
            float fillSweep = scale * ANGLE_SWEEP;
            fillPath.arcTo(arcRect, ANGLE_START, fillSweep);
            arcPath.arcTo(arcRect, ANGLE_START + fillSweep, ANGLE_SWEEP - fillSweep);
        } else {
            arcPath.arcTo(arcRect, ANGLE_START, ANGLE_SWEEP);
        }

        if (pressed) {
            paint.setColor(pressedColor);
            paint.setStyle(Paint.Style.FILL);
            arcRect.set(0, 0, width, height);
            canvas.drawOval(arcRect, paint);

            paint.setStyle(Paint.Style.STROKE);
        }

        paint.setColor(trackColor);
        canvas.drawPath(arcPath, paint);

        paint.setColor(fillColor);
        canvas.drawPath(fillPath, paint);

        if (labelPaint != null) {
            canvas.drawText(label, 0, label.length(), width / 2f, height - labelHeight, labelPaint);
        }
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean pressed = false;
        for (int state : stateSet) {
            if (state == android.R.attr.state_pressed) {
                pressed = true;
            }
        }

        if (pressed != this.pressed) {
            this.pressed = pressed;
            invalidateSelf();
            return true;
        }

        return false;
    }


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        if (alpha != this.alpha) {
            this.alpha = alpha;
            this.fillColor = Drawing.colorWithAlpha(fillColor, alpha);
            this.trackColor = Drawing.colorWithAlpha(trackColor, alpha);

            if (labelPaint != null) {
                labelPaint.setAlpha(alpha);
            }

            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        if (labelPaint != null) {
            labelPaint.setColorFilter(colorFilter);
        }
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_width);
    }

    @Override
    public int getIntrinsicHeight() {
        return resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_height);
    }

    @Override
    public boolean isStateful() {
        return stateful;
    }

    public void setStateful(boolean stateful) {
        this.stateful = stateful;
    }

    public int getPressedColor() {
        return pressedColor;
    }

    public void setTrackColor(int trackColor) {
        this.trackColor = Drawing.colorWithAlpha(trackColor, alpha);
        invalidateSelf();
    }

    public void setValue(int value) {
        this.value = (float) value;
        invalidateSelf();
    }

    public int getValue() {
        return (int) value;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = Drawing.colorWithAlpha(fillColor, alpha);
        invalidateSelf();
    }

    public int getFillColor() {
        return fillColor;
    }

    //endregion
}
