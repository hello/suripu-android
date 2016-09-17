package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.TypedValue;


import is.hello.sense.R;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.ui.widget.util.Drawing;


public class SensorGraphDrawable extends Drawable {
    /**
     * This will determine the stroke width based on height of the graph to scale on larger devices.
     */
    private static final float STROKE_RATIO = .01f;

    /**
     * Will scale height so  novalues (except -1) can fall below.
     */
    private static final float MIN_HEIGHT_RATIO = .9f;
    /**
     * Distance from bottom and top to draw from.
     */
    private static final float TEXT_POSITION_RATIO = .14f;


    private final Sensor sensor;
    private final int height;
    private final int minHeight;
    private final float scaleRatio;
    private float scaleFactor = 1;
    private final float strokeWidth;
    private final float textPositionOffset;

    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public SensorGraphDrawable(@NonNull final Context context, @NonNull final Sensor sensor) {
        Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.AppTheme_Text_Trends_BarGraph);
        this.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics());
        this.strokeWidth = this.height * STROKE_RATIO;
        this.sensor = sensor;
        final int strokeColor = sensor.getColor(context);
        final int topColor = Color.argb(80, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
        final int bottomColor = Color.argb(30, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
        this.gradientPaint.setShader(new LinearGradient(0, 0, 0, height, topColor, bottomColor, Shader.TileMode.MIRROR));
        this.strokePaint.setColor(strokeColor);
        this.minHeight = (int) ((height * MIN_HEIGHT_RATIO) + strokeWidth * 2);
        textPositionOffset = height * TEXT_POSITION_RATIO;
        final float max = sensor.getValueLimits().getMax();
        if (max >= 1) {
            this.scaleRatio = minHeight / sensor.getValueLimits().getMax();
        } else {
            this.scaleRatio = 0;
        }

    }

    public final void setScaleFactor(final float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public void draw(final Canvas canvas) {
        final float[] values = this.sensor.getSensorValues();
        final Path path = new Path();
        final double width = canvas.getWidth();
        final double xIncrement = width / values.length + 1;
        int drawTo = (int) ((this.scaleFactor * width) / xIncrement);
        if (drawTo > values.length) {
            drawTo = values.length;
        }
        path.moveTo(-10, canvas.getHeight() * 2);// arbitrary value greater than height to make sure there isn't a stroke at the bottom of the graph
        for (int i = 0; i < drawTo; i++) {
            final float value = values[i];
            final float x = (float) (i * xIncrement);
            final float y;
            if (value != -1) {
                y = this.minHeight - value * this.scaleRatio;
            } else {
                y = this.height + strokeWidth * 2;
            }
            path.lineTo(x, y);
        }
        path.lineTo((float) width * this.scaleFactor, canvas.getHeight());
        path.lineTo(-10, canvas.getHeight() * 2); //  arbitrary value greater than height to make sure there isn't a stroke at the bottom of the graph
        this.gradientPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, this.gradientPaint);
        this.strokePaint.setStyle(Paint.Style.STROKE);
        this.strokePaint.setStrokeWidth(strokeWidth);
        canvas.drawPath(path, this.strokePaint);

        Scale minScale = sensor.getMinScale();
        Scale maxScale = sensor.getMaxScale();
        if (minScale == null && maxScale == null) {
            minScale = sensor.getMinScaleFromAvailable();
            maxScale = sensor.getMaxScaleFromAvailable();
        } else if (minScale == null) {
            minScale = sensor.getScaleFor(maxScale);
        } else if (maxScale == null) {
            maxScale = sensor.getScaleFor(minScale);
        } else if (minScale.equals(maxScale)) {
            if (minScale.equals(sensor.getMinScaleFromAvailable())) {
                maxScale = sensor.getScaleFor(minScale);
            } else {
                minScale = sensor.getScaleFor(maxScale);
            }
        }

        if (minScale != null) {
            canvas.drawText(minScale.getName(), (float) (width * .05f), minHeight - textPositionOffset, textLabelPaint);
        }

        if (maxScale != null) {
            canvas.drawText(maxScale.getName(), (float) (width * .05f), textPositionOffset, textLabelPaint);
        }


    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

}
