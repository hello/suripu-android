package is.hello.sense.ui.widget.graphing.sensors;

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
import android.util.TypedValue;

import java.util.Random;

import is.hello.sense.api.model.v2.sensors.Sensor;

public class SensorGraphDrawable extends Drawable {
    private final Sensor sensor;
    private final int height;
    private final float scaleRatio;
    private final ValueLimits valueLimits;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SensorGraphDrawable(@NonNull final Context context, @NonNull final Sensor sensor) {
        this.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics());
        this.sensor = sensor;
        final int color = sensor.getColor(context);
        this.paint.setColor(color);
        this.paint.setAlpha(200);
        this.edgePaint.setColor(color);
        this.edgePaint.setAlpha(255);
        this.valueLimits = new ValueLimits(sensor.getSensorValues());
        this.scaleRatio = height / valueLimits.max;


    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public void draw(final Canvas canvas) {
        final float[] values = sensor.getSensorValues();
        final float xIncrement = values.length / canvas.getWidth();

        for (int i = 0; i < values.length; i++) {
            final float value = values[i];
            final RectF rectF = new RectF();
            rectF.left = xIncrement * i;
            rectF.right = rectF.left + xIncrement;
            rectF.top = new Random().nextInt(50) + 30;
            rectF.bottom = canvas.getHeight();
            final RectF edgeRectF = new RectF(rectF);
            edgeRectF.bottom = edgeRectF.top;
            edgeRectF.top -= 10;
            canvas.drawRect(rectF, paint);
            canvas.drawRect(edgeRectF, edgePaint);
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
