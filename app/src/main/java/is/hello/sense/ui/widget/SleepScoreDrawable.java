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

import is.hello.sense.R;

public class SleepScoreDrawable extends Drawable {
    private static final int MAX_VALUE = 100;

    private final Path fillPath = new Path();
    private final RectF arcRect = new RectF();
    private final Paint paint = new Paint();
    private final float fillStrokeWidth;

    private int value = 0;
    private int fillColor;
    private int trackColor;


    public SleepScoreDrawable(@NonNull Resources resources) {
        this.fillStrokeWidth = resources.getDimensionPixelSize(R.dimen.pie_graph_stroke_width);
        this.fillColor = Color.TRANSPARENT;
        this.trackColor = resources.getColor(R.color.border);

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(fillStrokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        fillPath.reset();

        int width = canvas.getWidth(), height = canvas.getHeight();
        int size = Math.min(width, height);

        float left = width > height ? (width - height) / 2 : 0;
        float top = width < height ? (height - width) / 2 : 0;

        arcRect.set(left, top, size + left, size + top);
        arcRect.inset(fillStrokeWidth / 2f, fillStrokeWidth / 2f);

        float scale = ((float) value / (float) MAX_VALUE);
        if (scale > 0f) {
            fillPath.moveTo(arcRect.centerX(), arcRect.top);
            fillPath.arcTo(arcRect, -90f, scale * 360f);
        }

        canvas.save();
        {
            paint.setColor(trackColor);
            canvas.drawOval(arcRect, paint);

            paint.setColor(fillColor);
            canvas.drawPath(fillPath, paint);

        }
        canvas.restore();
    }


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
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
