package is.hello.sense.ui.widget.util;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.ColorInt;

import com.squareup.picasso.Transformation;

public class RoundedCornersTransformation implements Transformation {

    private final int radius;
    private final int borderWidth;
    @ColorInt
    private final int borderColor;
    private final String key;

    public RoundedCornersTransformation(final int radius,
                                        final int borderWidth,
                                        @ColorInt
                                        final int borderColor) {
        this.radius = radius;
        this.borderWidth = borderWidth;
        this.borderColor = borderColor;
        this.key = String.format("%s(radius: %s, margin: %s, cornerType: %s)",
                            RoundedCornersTransformation.class.getSimpleName(),
                            this.radius, this.borderWidth, this.borderColor);
    }

    @Override
    public Bitmap transform(final Bitmap source) {

        final int width = source.getWidth();
        final int height = source.getHeight();

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(bitmap);

        final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(borderColor);
        drawRoundRect(canvas, backgroundPaint, width, height, 0);

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(source,
                                         Shader.TileMode.CLAMP,
                                         Shader.TileMode.CLAMP));
        drawRoundRect(canvas, paint, width, height, borderWidth);
        source.recycle();

        return bitmap;
    }

    private void drawRoundRect(final Canvas canvas,
                               final Paint paint,
                               final float width,
                               final float height,
                               final int margin) {
        final float right = width - margin;
        final float bottom = height - margin;
        canvas.drawRoundRect(new RectF(margin, margin, right, bottom), radius, radius, paint);
    }

    @Override
    public String key() {
        return key;
    }
}