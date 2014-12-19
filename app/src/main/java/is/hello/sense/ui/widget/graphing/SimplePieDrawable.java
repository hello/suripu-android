package is.hello.sense.ui.widget.graphing;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class SimplePieDrawable extends Drawable {
    private final Path fillPath = new Path();
    private final RectF arcRect = new RectF();
    private final Paint paint = new Paint();

    private int maxValue = 100;
    private int value = 0;

    private int fillColor;
    private float fillStrokeWidth;
    private int trackColor;


    public SimplePieDrawable(@NonNull Resources resources) {
        this.fillStrokeWidth = resources.getDimensionPixelSize(R.dimen.pie_graph_stroke_width);
        this.fillColor = resources.getColor(R.color.graph_fill_accent_color);
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

        float scale = ((float) value / (float) maxValue);
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

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        invalidateSelf();
    }

    public void setValue(int value) {
        this.value = value;
        invalidateSelf();
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        invalidateSelf();
    }

    //endregion
}
