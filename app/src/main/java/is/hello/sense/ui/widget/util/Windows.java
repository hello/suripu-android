package is.hello.sense.ui.widget.util;

import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.Window;

public class Windows {
    public static void setStatusBarColor(@NonNull Window window, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(color);
        }
    }

    public static @ColorInt int getStatusBarColor(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return window.getStatusBarColor();
        } else {
            return Color.BLACK;
        }
    }

    public static AnimatorUpdateListener createStatusBarUpdateListener(@NonNull Window forWindow) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return animator -> {
                final @ColorInt int color = (int) animator.getAnimatedValue();
                forWindow.setStatusBarColor(color);
            };
        } else {
            return ignored -> {};
        }
    }

    public static boolean isStatusBarColorAvailable() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }
}
