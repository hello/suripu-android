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
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import is.hello.sense.R;
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
    private static final float MIN_HEIGHT_RATIO = .7f;
    /**
     * Distance from bottom and top to draw from.
     */
    private static final float TEXT_Y_POSITION_RATIO = .14f;
    /**
     * Distance from left and right to draw from.
     */
    private static final float TEXT_X_POSITION_RATIO = .05f;
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
    private final int maxHeight;
    private final int lineDistance;
    private final float strokeWidth;
    private final float textPositionOffset;
    private final float valueDifference;
    private final float minValue;
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private float scaleFactor = 0; // Animation

    public SensorGraphDrawable(@NonNull final Context context, @NonNull final Sensor sensor) {
        this.sensor = sensor;
        this.minValue = sensor.getValueLimits().getMin();
        this.valueDifference = sensor.getValueLimits().getMax() - minValue;

        // Sizes
        this.height = context.getResources().getDimensionPixelSize(R.dimen.sensor_graph_height);
        this.lineDistance = context.getResources().getDimensionPixelSize(R.dimen.sensor_line_distance);
        this.strokeWidth = this.height * STROKE_RATIO;
        this.minHeight = (int) ((this.height * MIN_HEIGHT_RATIO) + (this.strokeWidth * 2));
        this.textPositionOffset = this.height * TEXT_Y_POSITION_RATIO;
        this.maxHeight = (int) (textPositionOffset + lineDistance);
        final float max = sensor.getValueLimits().getMax();

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
        if (scaleFactor > 1) {
            if (this.scaleFactor != 1) {
                this.scaleFactor = 1;
                invalidateSelf();
            }
        } else {
            this.scaleFactor = scaleFactor;
            invalidateSelf();
        }
    }

    @Override
    public int getIntrinsicHeight() {
        return this.height;
    }

    /**
     * Something to remember is the canvas is drawn upside down. The highest point of the graph is 0
     * and the lowest point of the graph is equal to the canvas height. So the smaller y is, the taller
     * the graph is.
     * <p>
     * (0,0)                           (canvas.getWidth(), 0)
     * _____________________________
     * |                _____        |
     * |  __/\    /\___/     \      _|
     * |_/    \__/            \____/ |
     * |_____________________________|
     * (0, canvas.getHeight())         (canvas.getWidth(), canvas.getHeight())
     */
    @Override
    public void draw(final Canvas canvas) {
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
            final float xPos = (float) (i * xIncrement);
            final float yPos = getValueHeight(value);
            path.lineTo(xPos, yPos);
        }

        // draw off the screen but keep the height equal to the last drawn point. This makes it so the graph doesn't drop in height near the end.
        path.lineTo((width + (this.strokeWidth * 2)) * this.scaleFactor, getValueHeight(values[drawTo]));

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
        if (value == 0) {
            return this.minHeight;
        } else if (value != -1) {
            return this.minHeight - (this.minHeight * ((value - minValue) / valueDifference)) + this.strokeWidth * 2 + maxHeight;
        } else {
            return this.height + this.strokeWidth * 2;
        }
    }

    private void drawScale(@NonNull final Canvas canvas) {
        final float width = canvas.getWidth();
        final Path path = new Path();
        final float xPos = width * TEXT_X_POSITION_RATIO;
        float yPos = this.minHeight + this.textPositionOffset;

        canvas.drawText(this.sensor.getValueLimits().getFormattedMin() + " " + this.sensor.getSensorSuffix(), xPos, yPos, this.textLabelPaint);
        yPos += this.lineDistance;
        path.moveTo(xPos, yPos);
        path.lineTo(width - xPos, yPos);
        canvas.drawPath(path, this.linePaint);
        if (this.sensor.getValueLimits().getFormattedMax().isEmpty()) {
            return;
        }
        yPos = this.textPositionOffset;
        canvas.drawText(this.sensor.getValueLimits().getFormattedMax() + " " + this.sensor.getSensorSuffix(), xPos, yPos, this.textLabelPaint);
        yPos += this.lineDistance;
        path.reset();
        path.moveTo(xPos, yPos);
        path.lineTo(width - xPos, yPos);
        canvas.drawPath(path, this.linePaint);

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
