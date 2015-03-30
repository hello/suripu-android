package is.hello.sense.ui.widget.graphing;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * The built-in ColorDrawable before API Level 21 (Lollipop) does not
 * support color filters, which we use extensively when rendering graphs.
 * This is a simplified copy of the built-in ColorDrawable that adds that
 * needed support.
 */
public class ColorDrawableCompat extends Drawable {
    private final Paint paint = new Paint();

    public ColorDrawableCompat(int color) {
        setColor(color);
    }


    //region Drawing

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), paint);
    }

    //endregion


    //region Properties

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        if (paint.getColorFilter() != null) {
            return PixelFormat.TRANSLUCENT;
        }

        switch (Color.alpha(paint.getColor())) {
            case 255:
                return PixelFormat.OPAQUE;

            case 0:
                return PixelFormat.TRANSPARENT;

            default:
                return PixelFormat.TRANSLUCENT;
        }
    }


    public void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }

    public int getColor() {
        return paint.getColor();
    }

    //endregion
}
