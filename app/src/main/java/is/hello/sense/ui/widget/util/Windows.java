package is.hello.sense.ui.widget.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.Window;

import is.hello.go99.animators.AnimatorTemplate;

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // added to pass lint
                    final @ColorInt int color = (int) animator.getAnimatedValue();
                    forWindow.setStatusBarColor(color);
                }
            };
        } else {
            return ignored -> {};
        }
    }

    public static Animator createStatusBarColorAnimator(@NonNull Window forWindow,
                                                        @NonNull @ColorInt int... colors) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final ValueAnimator animator =
                    AnimatorTemplate.DEFAULT.createColorAnimator((int[]) colors);
            animator.addUpdateListener(createStatusBarUpdateListener(forWindow));
            return animator;
        } else {
            return AnimatorTemplate.DEFAULT.apply(new AnimatorSet());
        }
    }

    public static boolean isStatusBarColorAvailable() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }
}
