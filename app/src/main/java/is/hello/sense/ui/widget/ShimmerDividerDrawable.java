package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import is.hello.go99.Anime;
import is.hello.sense.R;

public class ShimmerDividerDrawable extends Drawable implements Animatable {
    private static final int FRAME_DURATION = 16;
    private static final int DURATION_DEFAULT = 1600;

    private final Paint backgroundPaint = new Paint();

    private final GradientDrawable shimmerGradient;
    private final int intrinsicHeight;

    private float offset = 0f;
    private boolean running = false;
    private float increment;

    public ShimmerDividerDrawable(@ColorInt int backgroundColor,
                                  @ColorInt int shimmerColor,
                                  int intrinsicHeight) {
        backgroundPaint.setColor(backgroundColor);

        final @ColorInt int[] colors = {
                Color.TRANSPARENT,
                shimmerColor,
                Color.TRANSPARENT,
        };
        this.shimmerGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        this.intrinsicHeight = intrinsicHeight;

        setDuration(DURATION_DEFAULT);
    }

    public static ShimmerDividerDrawable createTrendCardDivider(@NonNull Resources resources) {
        return new ShimmerDividerDrawable(resources.getColor(R.color.trends_line_divider),
                                          resources.getColor(R.color.trends_line_divider_shimmer),
                                          resources.getDimensionPixelSize(R.dimen.divider_size_thick));
    }


    //region Metrics

    @Override
    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    //endregion


    //region Drawing

    @Override
    public void draw(Canvas canvas) {
        final Rect bounds = getBounds();
        canvas.drawRect(bounds, backgroundPaint);

        if (running) {
            final int saveCount = canvas.save();
            canvas.clipRect(bounds);

            final int shimmerWidth = (bounds.width() / 2);
            final int left = (int) Anime.interpolateFloats(offset,
                                                           bounds.left - shimmerWidth,
                                                           bounds.right + shimmerWidth);
            shimmerGradient.setBounds(left, bounds.top, left + shimmerWidth, bounds.bottom);
            shimmerGradient.draw(canvas);

            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        backgroundPaint.setAlpha(alpha);
        shimmerGradient.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        shimmerGradient.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    //endregion


    //region Animation

    public void setDuration(int duration) {
        this.increment = ((float) FRAME_DURATION / (float) duration);
    }

    @Override
    public void start() {
        this.running = true;
        nextFrame();
    }

    @Override
    public void stop() {
        this.running = false;
        this.offset = 0f;
        unscheduleSelf(nextFrame);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void nextFrame() {
        unscheduleSelf(nextFrame);
        scheduleSelf(nextFrame, SystemClock.uptimeMillis() + FRAME_DURATION);
    }

    private final Runnable nextFrame = () -> {
        this.offset += increment;
        if (offset > 1f) {
            this.offset = 0f;
        }

        invalidateSelf();
        nextFrame();
    };

    //endregion
}
