package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.text.TextPaint;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.units.UnitFormatter;


public class SensorGraphDrawable extends Drawable {
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
     * Width of graph lines
     */
    private static final float STROKE_WIDTH = 2f;
    /**
     * Width of limit lines
     */
    private static final float LIMIT_LINE_WIDTH = 1f;
    /**
     * White portion of scrubber
     */
    private static final float SCRUBBER_OUTER_RADIUS = 10;
    /**
     * Colored portion of scrubber.
     */
    private static final float SCRUBBER_INNER_RADIUS = 6;

    private static final float EXTRA_OFF_SCREEN_PX = 100;
    /**
     * How rounded the corners joining adjacent points should be
     */
    private static final PathEffect CORNER_PATH_EFFECT = new CornerPathEffect(10);

    public static final int NO_LOCATION = -1;

    private final Sensor sensor;
    private final int height;
    private final int minHeight;
    private final int maxHeight;
    private final int lineDistance;
    private final float textPositionOffset;
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrubberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path drawingPath = new Path();
    private final ValueLimits limits;
    private final String[] labels;
    private final String formattedMin;
    private final String formattedMax;

    private float scrubberLocation = NO_LOCATION;
    private float scaleFactor = 0; // Animation
    private ScrubberCallback scrubberCallback = null;

    public SensorGraphDrawable(@NonNull final Context context,
                               @NonNull final Sensor sensor,
                               @NonNull final UnitFormatter unitFormatter,
                               final int height) {
        this(context, sensor, unitFormatter, height, null);
    }

    public SensorGraphDrawable(@NonNull final Context context,
                               @NonNull final Sensor sensor,
                               @NonNull final UnitFormatter unitFormatter,
                               final int height,
                               @Nullable final String[] labels) {
        this.sensor = sensor;
        this.limits = new ValueLimits(sensor.getSensorValues());
        this.labels = labels;
        this.formattedMin = unitFormatter.createUnitBuilder(sensor.getType(), this.limits.min)
                                         .buildWithStyle()
                                         .toString();
        this.formattedMax = unitFormatter.createUnitBuilder(sensor.getType(), this.limits.max)
                                         .buildWithStyle()
                                         .toString();
        // Sizes
        final Resources resources = context.getResources();
        this.height = height;
        this.lineDistance = resources.getDimensionPixelSize(R.dimen.sensor_line_distance);
        this.textPositionOffset = this.height * TEXT_Y_POSITION_RATIO;
        this.minHeight = (int) ((this.height * MIN_HEIGHT_RATIO));
        this.maxHeight = (int) (textPositionOffset + lineDistance);

        // Paints
        @ColorInt
        final int strokeColor = ContextCompat.getColor(context, sensor.getColor());

        @ColorInt
        final int gradientColor;
        if (resources.getBoolean(R.bool.sensor_graph_gradient_matches_stroke_color)) {
            gradientColor = strokeColor;
        } else {
            gradientColor = ContextCompat.getColor(context, R.color.sensor_graph_gradient);
        }
        final int topGradientAlpha = resources.getInteger(R.integer.sensor_graph_gradient_top_alpha);
        final int bottomGradientAlpha = resources.getInteger(R.integer.sensor_graph_gradient_bottom_alpha);
        this.gradientPaint.setShader(new LinearGradient(0, this.maxHeight, 0, this.height,
                                                        ColorUtils.setAlphaComponent(gradientColor, topGradientAlpha),
                                                        ColorUtils.setAlphaComponent(gradientColor, bottomGradientAlpha),
                                                        Shader.TileMode.MIRROR));
        this.gradientPaint.setStyle(Paint.Style.FILL);

        this.strokePaint.setColor(strokeColor);
        this.strokePaint.setStyle(Paint.Style.STROKE);
        this.strokePaint.setStrokeWidth(STROKE_WIDTH);
        this.strokePaint.setStrokeCap(Paint.Cap.ROUND);
        this.strokePaint.setStrokeJoin(Paint.Join.ROUND);
        this.strokePaint.setPathEffect(CORNER_PATH_EFFECT);
        //this.strokePaint.setDither(true);

        this.linePaint.setColor(ContextCompat.getColor(context, R.color.divider));
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeWidth(LIMIT_LINE_WIDTH);

        this.scrubberPaint.setColor(strokeColor);
        this.scrubberPaint.setStyle(Paint.Style.FILL);

        this.whiteFillPaint.setColor(Color.WHITE);
        this.whiteFillPaint.setStyle(Paint.Style.FILL);
        Drawing.updateTextPaintFromStyle(this.textLabelPaint, context, R.style.Caption1_Hint);
    }

    public final void setScaleFactor(final float scaleFactor) {
        if (scaleFactor < 0) {
            return;
        }
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
     * ______________________________
     * |                _____        |
     * |  __/\    /\___/     \      _|
     * |_/    \__/            \____/ |
     * |_____________________________|
     * (0, canvas.getHeight())         (canvas.getWidth(), canvas.getHeight())
     */
    @Override
    public void draw(@NonNull final Canvas canvas) {
        this.drawingPath.reset();
        final float[] values = this.sensor.getSensorValues();
        if (values == null || values.length == 0) {
            return;
        }
        // Cast once
        final float maxWidth = Math.min(canvas.getWidth() + EXTRA_OFF_SCREEN_PX, canvas.getMaximumBitmapWidth());
        final float maxHeight = Math.min(this.height + EXTRA_OFF_SCREEN_PX, canvas.getMaximumBitmapHeight());

        // distance between each value point;
        final double xIncrement = maxWidth / values.length;

        // determine how many values to draw for a smooth animation effect
        int drawTo = (int) ((this.scaleFactor * maxWidth) / xIncrement);
        if (drawTo >= values.length) {
            drawTo = values.length - 1;
        }
        // start the line off the screen.
        this.drawingPath.moveTo(-maxWidth, maxHeight);

        for (int i = 0; i < drawTo; i++) {
            this.drawingPath.lineTo((float) (i * xIncrement), getValueHeight(values[i]));
        }

        // draw off the screen but keep the height equal to the last drawn point. This makes it so the graph doesn't drop in height near the end.
        this.drawingPath.lineTo(maxWidth * this.scaleFactor, getValueHeight(values[drawTo]));

        // Draw the darker colored stroke around the graph.
        canvas.drawPath(this.drawingPath, this.strokePaint);

        // Now draw below the graph.
        this.drawingPath.lineTo(maxWidth * this.scaleFactor, maxHeight);

        // Connect back to the start so it fills nicely.
        this.drawingPath.lineTo(-maxWidth, maxHeight);

        // offset so gradient draws under the stroke
        this.drawingPath.offset(0, STROKE_WIDTH);

        // Fill the graph with the light colored gradient.
        canvas.drawPath(this.drawingPath, this.gradientPaint);

        // Draw the min/max text and horizontal line.
        drawScale(canvas);
        drawScrubber(canvas, xIncrement);
        drawText(canvas);
    }

    private float getValueHeight(final float value) {
        if (value == 0) {
            return this.minHeight + this.textPositionOffset + this.lineDistance; // location for bottom line
        } else if (value != Sensor.NO_VALUE) {
            return this.minHeight
                    - (this.minHeight * ((value - this.limits.min) / this.limits.valueDifference))
                    + this.maxHeight;
        } else {
            return this.height + EXTRA_OFF_SCREEN_PX;
        }
    }

    private void drawText(@NonNull final Canvas canvas) {
        if (labels == null) {
            return;
        }
        final Rect rect = new Rect();
        textLabelPaint.getTextBounds(labels[0], 0, labels[0].length(), rect);
        final float xInc = canvas.getWidth() / labels.length;
        final float textHeight = textLabelPaint.getTextSize();
        final float offset = Math.abs((xInc - rect.width())) / 2;

        for (int i = 0; i < labels.length; i++) {
            canvas.drawText(labels[i], xInc * i + offset, canvas.getHeight() - textHeight, textLabelPaint);
        }
    }

    private void drawScrubber(@NonNull final Canvas canvas, final double xInc) {
        if (scrubberLocation == NO_LOCATION) {
            return;
        }

        final int position = (int) (scrubberLocation / xInc);
        if (position < 0 || position > sensor.getSensorValues().length - 1) {
            return;
        }

        final float yLoc = getValueHeight(sensor.getSensorValues()[position]);

        drawingPath.reset();
        drawingPath.moveTo(scrubberLocation, lineDistance + minHeight + this.textPositionOffset);
        drawingPath.lineTo(scrubberLocation, lineDistance + this.textPositionOffset);
        canvas.drawCircle(scrubberLocation, yLoc, SCRUBBER_OUTER_RADIUS, whiteFillPaint);
        canvas.drawCircle(scrubberLocation, yLoc, SCRUBBER_INNER_RADIUS, scrubberPaint);
        canvas.drawPath(drawingPath, strokePaint);

        if (scrubberCallback != null) {
            scrubberCallback.onPositionScrubbed(position);
        }
    }

    private void drawScale(@NonNull final Canvas canvas) {
        if (this.limits.min == Sensor.NO_VALUE) {
            return;
        }
        final float width = canvas.getWidth();
        this.drawingPath.reset();
        final float xPos = width * TEXT_X_POSITION_RATIO;
        float yPos = this.minHeight + this.textPositionOffset;
        canvas.drawText(formattedMin,
                        xPos,
                        yPos,
                        this.textLabelPaint);
        yPos += this.lineDistance;
        this.drawingPath.moveTo(xPos, yPos);
        this.drawingPath.lineTo(width - xPos, yPos);
        canvas.drawPath(this.drawingPath, this.linePaint);
        if (this.limits.max == Sensor.NO_VALUE) {
            return;
        }
        yPos = this.textPositionOffset;
        canvas.drawText(formattedMax,
                        xPos,
                        yPos,
                        this.textLabelPaint);
        yPos += this.lineDistance;
        this.drawingPath.reset();
        this.drawingPath.moveTo(xPos, yPos);
        this.drawingPath.lineTo(width - xPos, yPos);
        canvas.drawPath(this.drawingPath, this.linePaint);
    }


    @Override
    public void setAlpha(final int alpha) {

    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setScrubberLocation(final float xLoc) {
        this.scrubberLocation = xLoc;
        invalidateSelf();
    }

    public void setScrubberCallback(@Nullable final ScrubberCallback scrubberCallback) {
        this.scrubberCallback = scrubberCallback;
    }

    private class ValueLimits {
        private float min = 0f;
        private float max = 0f;
        private final float valueDifference;

        public ValueLimits(@NonNull final float[] sensorValues) {
            min = (float) Sensor.NO_VALUE;
            max = (float) Sensor.NO_VALUE;
            if (sensorValues.length > 0) {
                for (final float sensorValue : sensorValues) {
                    if (sensorValue == Sensor.NO_VALUE) {
                        continue;
                    }
                    if (min == Sensor.NO_VALUE) {
                        min = sensorValue;
                        max = sensorValue;
                        continue;
                    }
                    if (sensorValue < min) {
                        min = sensorValue;
                        continue;
                    }
                    if (sensorValue > max) {
                        max = sensorValue;
                    }
                }
            }

            if (max == min) {
                max += 1;
            }
            this.valueDifference = max - min;

        }
    }

    public interface ScrubberCallback {
        void onPositionScrubbed(final int position);

        void onScrubberReleased();
    }

}
