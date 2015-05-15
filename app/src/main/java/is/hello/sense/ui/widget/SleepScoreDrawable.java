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

    private static final int MAX_VALUE = 100;

    private final Resources resources;

    private final Path fillPath = new Path();
    private final Path arcPath = new Path();
    private final RectF arcRect = new RectF();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float fillStrokeWidth;

    private final TextPaint labelPaint;
    private final String label;
    private final int labelHeight;

    private int value = 0;
    private int fillColor;
    private int trackColor;


    public SleepScoreDrawable(@NonNull Resources resources, boolean wantsLabel) {
        this.resources = resources;
        this.fillStrokeWidth = resources.getDimensionPixelSize(R.dimen.pie_graph_stroke_width);
        this.fillColor = Color.TRANSPARENT;
        this.trackColor = resources.getColor(R.color.border);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(fillStrokeWidth);

        if (wantsLabel) {
            this.labelPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | TextPaint.SUBPIXEL_TEXT_FLAG);
            labelPaint.setTextSize(resources.getDimension(R.dimen.text_size_section_heading));
            labelPaint.setColor(resources.getColor(R.color.text_section_header));
            labelPaint.setTextAlign(Paint.Align.CENTER);

            this.label = resources.getString(R.string.sleep_score).toUpperCase();

            this.labelHeight = Drawing.getEstimatedTextHeight(labelPaint);
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

        arcPath.arcTo(arcRect, ANGLE_START, ANGLE_SWEEP);

        float scale = ((float) value / (float) MAX_VALUE);
        if (scale > 0f) {
            fillPath.arcTo(arcRect, ANGLE_START, scale * ANGLE_SWEEP);
        }

        int savedAlpha = paint.getAlpha();
        paint.setColor(trackColor);
        paint.setAlpha(savedAlpha);
        canvas.drawPath(arcPath, paint);

        paint.setColor(fillColor);
        paint.setAlpha(savedAlpha);
        canvas.drawPath(fillPath, paint);

        if (labelPaint != null) {
            canvas.drawText(label, 0, label.length(), width / 2f, height - labelHeight, labelPaint);
        }
    }


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        if (alpha != paint.getAlpha()) {
            paint.setAlpha(alpha);
            if (labelPaint != null) {
                labelPaint.setAlpha(alpha);
            }
            invalidateSelf();
        }
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

    public void setTrackColor(int trackColor) {
        this.trackColor = trackColor;
        invalidateSelf();
    }

    public void setValue(int value) {
        this.value = value;
        invalidateSelf();
    }

    public int getValue() {
        return value;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        invalidateSelf();
    }

    //endregion
}
