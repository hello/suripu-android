package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import is.hello.go99.animators.AnimatorTemplate;

/**
 * The built-in ColorDrawable before API Level 21 (Lollipop) does not
 * support color filters, which we use extensively when rendering graphs.
 * This is a simplified copy of the built-in ColorDrawable that adds that
 * needed support.
 */
public class ColorDrawableCompat extends Drawable {
    private final Paint paint = new Paint();
    private @Nullable ValueAnimator currentAnimator;

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

    public Animator colorAnimator(int newColor) {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        this.currentAnimator = AnimatorTemplate.DEFAULT.createColorAnimator(getColor(), newColor);
        currentAnimator.addUpdateListener(a -> {
            int color = (int) a.getAnimatedValue();
            paint.setColor(color);
            invalidateSelf();
        });
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation == currentAnimator) {
                    setColor(newColor);
                    ColorDrawableCompat.this.currentAnimator = null;
                }
            }
        });

        return currentAnimator;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (super.setVisible(visible, restart)) {
            if (!visible && currentAnimator != null) {
                currentAnimator.cancel();
            }

            return true;
        } else {
            return false;
        }
    }

    //endregion
}
