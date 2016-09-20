package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.TypedValue;


import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.ui.widget.util.Drawing;


public class SensorGraphDrawable extends Drawable {
    /**
     * This will determine the stroke width based on height of the graph to scale on larger devices.
     */
    private static final float STROKE_RATIO = .01f;

    /**
     * Will scale height so no values (except -1) will be drawn below.
     */
    private static final float MIN_HEIGHT_RATIO = .9f;
    /**
     * Distance from bottom and top to draw from.
     */
    private static final float TEXT_POSITION_RATIO = .14f;
    /**
     * Alpha value for the top of the gradient used to draw the graph.
     */
    private static final int GRADIENT_TOP_ALPHA = 100;
    /**
     * Alpha value for the bottom of the gradient used to draw the graph.
     */
    private static final int GRADIENT_BOTTOM_ALPHA = 30;
    /**
     * Width of lines
     */
    private static final int STROKE_WIDTH = 1;

    private final Sensor sensor;
    private final int height;
    private final int minHeight;
    private final float scaleRatio;
    private final float strokeWidth;
    private final float textPositionOffset;
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private float scaleFactor = 0; // Animation

    public SensorGraphDrawable(@NonNull final Context context, @NonNull final Sensor sensor) {
        this.sensor = sensor;

        // Sizes
        this.height = context.getResources().getDimensionPixelSize(R.dimen.sensor_graph_height);
        this.strokeWidth = this.height * STROKE_RATIO;
        this.minHeight = (int) ((this.height * MIN_HEIGHT_RATIO) + (this.strokeWidth * 2));
        this.textPositionOffset = this.height * TEXT_POSITION_RATIO;
        final float max = sensor.getValueLimits().getMax();
        if (max >= 1) { // If it's a fraction or negative the graph will not scale correctly.
            this.scaleRatio = minHeight / max;
        } else {
            this.scaleRatio = 0;
        }

        // Paints
        final int strokeColor = sensor.getColor(context);
        this.gradientPaint.setShader(new LinearGradient(0, 0, 0, this.height,
                                                        Color.argb(GRADIENT_TOP_ALPHA, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)),
                                                        Color.argb(GRADIENT_BOTTOM_ALPHA, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)),
                                                        Shader.TileMode.MIRROR));
        this.strokePaint.setColor(strokeColor);
        this.linePaint.setColor(ContextCompat.getColor(context, R.color.gray3));
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeWidth(STROKE_WIDTH);
        Drawing.updateTextPaintFromStyle(this.textLabelPaint, context, R.style.AppTheme_Text_Trends_BarGraph);
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
        /*
        Something to remember is the graphs are drawn upside down. The highest point of the graph is 0
        and the lowest point of the graph is equal to the canvas height. So the smaller y is, the taller
        the graph is.

        (0,0)                           (canvas.getWidth(), 0)
             ____________________________
            |                            |
            |                            |
            |                            |
            |____________________________|
        (0, canvas.getHeight())         (canvas.getWidth(), canvas.getHeight())


         */
        final Path path = new Path();
        final float[] values = this.sensor.getSensorValues();
        // Cast once
        final float width = canvas.getWidth();

        // distance between each value point;
        final double xIncrement = width / values.length;


        // determine how many values to draw for a smooth animation effect
        int drawTo = (int) ((this.scaleFactor * width) / xIncrement);
        if (drawTo >= values.length) {
            drawTo = values.length - 1;
        }
        // start the line off the screen.
        path.moveTo(-width, this.height * 2);


        for (int i = 0; i < drawTo; i++) {
            final float value = values[i];
            final float x = (float) (i * xIncrement);
            final float y = getValueHeight(value);
            path.lineTo(x, y);
        }
        // draw off the screen but keep the height equal to the last drawn point. This makes it so the graph doesn't drop in height near the end.
        path.lineTo((width + (this.strokeWidth * 2)) * this.scaleFactor, this.minHeight - values[drawTo] * this.scaleRatio);

        // Now draw below the graph.
        path.lineTo((width + (this.strokeWidth * 2)) * this.scaleFactor, this.height * 2);

        // Connect back to the start so it fills nicely.
        path.lineTo(-width, this.height * 2);

        // First fill the graph with the light colored gradient.
        this.gradientPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, this.gradientPaint);

        // Now draw the darker colored stroke around the graph.
        this.strokePaint.setStyle(Paint.Style.STROKE);
        this.strokePaint.setStrokeWidth(this.strokeWidth);
        canvas.drawPath(path, this.strokePaint);

        // Draw the min/max text and horizontal line.
        drawScale(canvas);

    }

    private float getValueHeight(final float value) {
        if (value != -1) {
            return this.minHeight - value * this.scaleRatio + this.strokeWidth * 2;
        } else {
            return this.height + this.strokeWidth * 2;
        }
    }

    private void drawScale(@NonNull final Canvas canvas) {
        final float width = canvas.getWidth();
        final Path path = new Path();
        final float drawX = width * .05f * scaleFactor;
        float drawY = minHeight - textPositionOffset;

        canvas.drawText(sensor.getValueLimits().getFormattedMin() + " " + sensor.getSensorSuffix(), drawX, drawY, textLabelPaint);
        drawY += 10;
        path.moveTo(drawX, drawY);
        path.lineTo((width - drawX) * scaleFactor, drawY);
        canvas.drawPath(path, linePaint);
        if (sensor.getValueLimits().getFormattedMax().isEmpty()) {
            return;
        }
        drawY = textPositionOffset;
        canvas.drawText(sensor.getValueLimits().getFormattedMax() + " " + sensor.getSensorSuffix(), drawX, drawY, textLabelPaint);
        drawY += 10;
        path.reset();
        path.moveTo(drawX, drawY);
        path.lineTo((width - drawX) * scaleFactor, drawY);
        canvas.drawPath(path, linePaint);

    }


    @Override
    public void setAlpha(final int alpha) {

    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

}
