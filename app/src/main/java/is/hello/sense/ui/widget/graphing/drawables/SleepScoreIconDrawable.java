package is.hello.sense.ui.widget.graphing.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;

import is.hello.sense.R;
import is.hello.sense.util.Constants;

/**
 * Create this with {@link is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable.Builder}
 */
//todo use this with new timeline navigation bar
public class SleepScoreIconDrawable extends Drawable {
    private static final int MDPI = 24;
    private static final int XHDPI = 48;
    private static final int XXHDPI = 72;
    private static final float CIRCLE_THICKNESS_RATIO = .1f;
    private static final float TEXT_MARGIN_RATIO = .5f;

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final int height;
    private final int width;
    private final String text;


    private SleepScoreIconDrawable(@NonNull final Builder builder) {
        final Context context = builder.context;
        final int selectedColor = ContextCompat.getColor(context, R.color.blue6);
        final int unselectedColor = ContextCompat.getColor(context, R.color.gray4);
        final int fillColor = ContextCompat.getColor(context, R.color.blue2);
        final int backgroundColor = ContextCompat.getColor(context, R.color.background_light);

        this.backgroundPaint.setColor(backgroundColor);
        this.width = builder.width;
        this.height = builder.height;
        this.text = builder.text;
        if (builder.isSelected) {
            this.circlePaint.setColor(selectedColor);
            this.textPaint.setColor(selectedColor);
            this.fillPaint.setColor(fillColor);
        } else {
            this.circlePaint.setColor(unselectedColor);
            this.textPaint.setColor(unselectedColor);
            this.fillPaint.setColor(backgroundColor);
        }
        getCorrectTextSize();
    }

    @Override
    public int getIntrinsicWidth() {
        return this.width;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.height;
    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.drawColor(this.backgroundPaint.getColor());
        final int centerX = canvas.getWidth() / 2;
        final int centerY = canvas.getHeight() / 2;
        final int radius;
        if (centerX > centerY) {
            radius = centerY;
        } else {
            radius = centerX;
        }
        canvas.drawCircle(centerX, centerY, radius, this.circlePaint);
        canvas.drawCircle(centerX, centerY, radius - radius * CIRCLE_THICKNESS_RATIO, this.fillPaint);
        drawAndCenterText(canvas, this.textPaint, this.text);


    }

    /**
     * This does nothing
     *
     * @param alpha
     */
    @Override
    public void setAlpha(final int alpha) {

    }

    /**
     * This does nothing
     *
     * @param colorFilter
     */
    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    /**
     * This does nothing
     */
    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }


    public void attachTo(@NonNull final ImageView imageView) {
        imageView.setBackground(this);
    }

    private void drawAndCenterText(final Canvas canvas,
                                   final Paint paint,
                                   final String text) {
        canvas.getClipBounds(this.textBounds);
        final float cHeight = this.textBounds.height();
        final float cWidth = this.textBounds.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), this.textBounds);
        final float x = cWidth / 2f - this.textBounds.width() / 2f - this.textBounds.left;
        final float y = cHeight / 2f + this.textBounds.height() / 2f - this.textBounds.bottom;
        canvas.drawText(text, x, y, paint);
    }

    private void getCorrectTextSize() {
        this.textPaint.setTextSize(0);
        final int height = getIntrinsicHeight() - (int) (getIntrinsicHeight() * TEXT_MARGIN_RATIO);
        final int width = getIntrinsicWidth() - (int) (getIntrinsicWidth() * TEXT_MARGIN_RATIO);
        while (doesTextFit(width, height)) {
            this.textPaint.setTextSize(this.textPaint.getTextSize() + 1);
        }
        // its ok to be bigger. We have lots of margin

    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean doesTextFit(final int width, final int height) {
        this.textPaint.getTextBounds(this.text, 0, this.text.length(), this.textBounds);
        if (this.textBounds.height() > height) {
            return false;
        }
        if (this.textBounds.width() > width) {
            return false;
        }
        return true;
    }

    public static class Builder {
        private final Context context;
        private String text = Constants.EMPTY_STRING;
        private boolean isSelected = false;
        private int height = 0;
        private int width = 0;

        public Builder(@NonNull final Context context) {
            this.context = context;
            text = context.getString(R.string.missing_data_placeholder);
        }

        public Builder withText(@NonNull final String text) {
            this.text = text;
            return this;
        }

        public Builder withText(final int score) {
            this.text = String.valueOf(score);
            return this;
        }

        public Builder withSelected(final boolean isSelected) {
            this.isSelected = isSelected;
            return this;
        }

        public Builder withSize(@NonNull final WindowManager windowManager) {
            final int size = getIconSize(windowManager);
            this.width = size;
            this.height = size;
            return this;
        }

        public Builder withSize(final int width,
                                final int height) {

            this.width = width;
            this.height = height;
            return this;
        }

        public SleepScoreIconDrawable build() {
            return new SleepScoreIconDrawable(this);
        }

    }

    public static int getIconSize(@NonNull final WindowManager windowManager) {
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        final int density = metrics.densityDpi;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
            case DisplayMetrics.DENSITY_MEDIUM:
                return MDPI;
            case DisplayMetrics.DENSITY_HIGH:
            case DisplayMetrics.DENSITY_XHIGH:
                return XHDPI;
            case DisplayMetrics.DENSITY_XXHIGH:
            default:
                return XXHDPI;
        }
    }
}
