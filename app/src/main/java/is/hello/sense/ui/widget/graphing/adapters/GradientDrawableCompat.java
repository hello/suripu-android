package is.hello.sense.ui.widget.graphing.adapters;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;

/**
 * Versions of Android older than KitKat have a bug where {@link GradientDrawable#setAlpha(int)}
 * does not work after you've called <code>setAlpha(0)</code>. This compatibility class replaces
 * the built-in alpha handling with an alpha-shifted canvas. As such it is very slow, and should
 * not be used on versions of Android that correctly implement alpha for gradients.
 * <p>
 * This class should be removed whenever we drop support for Jelly Bean MR2 (4.3).
 * It is preemptively marked as {@link Deprecated}.
 */
@Deprecated
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GradientDrawableCompat extends GradientDrawable {
    private int alpha = 255;

    public GradientDrawableCompat(@NonNull Orientation orientation,
                                  @NonNull int[] colors) {
        super(orientation, colors);
    }

    @Override
    public void draw(Canvas canvas) {
        // At least 2x slower. Yikes.
        final int save = canvas.saveLayerAlpha(0f, 0f, canvas.getWidth(), canvas.getHeight(),
                                               alpha, Canvas.ALL_SAVE_FLAG);
        super.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public int getOpacity() {
        return alpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }
}
