package is.hello.sense.ui.widget.graphing.sensors;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;

import java.util.Random;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.sensors.Sensor;

public class SensorGraphDrawable extends Drawable {
    private final Sensor sensor;
    private final int height;
    private final int minHeight;
    private final float scaleRatio;
    private final ValueLimits valueLimits;
    private float scaleFactor = 0;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SensorGraphDrawable(@NonNull final Context context, @NonNull final Sensor sensor) {
        this.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics());
        this.sensor = sensor;
        final int color = sensor.getColor(context);
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.FILL);
        this.valueLimits = new ValueLimits(sensor.getSensorValues());
        this.minHeight = (int) (height * .99f);
        this.scaleRatio = minHeight / valueLimits.max;
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
        final float[] values = sensor.getSensorValues();
        final Path path = new Path();
        final double width = canvas.getWidth() * scaleFactor;
        final double xIncrement = width / values.length + 1;

        path.moveTo(0, canvas.getHeight());
        for (int i = 0; i < values.length; i++) {
            final float value = values[i];
            final float x = (float) (i * xIncrement);
            final float y = minHeight - value * scaleRatio;
            path.lineTo(x, y);
        }
        path.lineTo((float) width, canvas.getHeight());
        path.lineTo(0, canvas.getHeight());
        canvas.drawPath(path, paint);

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


    private class ValueLimits {
        private float min, max;

        public ValueLimits(@NonNull final float[] values) {
            for (final Float value : values) {
                if (value < min) {
                    min = value;
                    continue;
                }
                if (value > max) {
                    max = value;
                }
            }
        }
    }
}
