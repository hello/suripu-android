package is.hello.sense.ui.widget.util;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;

public final class Drawables {
    /**
     * Specifies a tint for a given drawable.
     * <p/>
     * Unlike {@link android.support.v4.graphics.drawable.DrawableCompat},
     * this compatibility implementation behaves the same on all versions.
     * <p/>
     * Setting a color filter on the drawable will clear any tint color.
     */
    public static void setTintColor(@NonNull Drawable drawable, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable.setTint(color);
        } else {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }
}
