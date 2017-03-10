package is.hello.sense.ui.widget.graphing.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.widget.ImageView;

import is.hello.sense.R;
import is.hello.sense.util.Constants;

import static is.hello.sense.util.PaintUtil.drawAndCenterText;
import static is.hello.sense.util.PaintUtil.getCorrectTextSize;

/**
 * Create this with {@link is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable.Builder}
 */
//todo use this with new timeline navigation bar
public class SleepScoreIconDrawable extends Drawable {
    private static final float CIRCLE_THICKNESS_RATIO = .1f;
    private static final float TEXT_MARGIN_RATIO = .5f;

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        getCorrectTextSize(textPaint,
                           text,
                           getIntrinsicWidth() - (int) (getIntrinsicWidth() * TEXT_MARGIN_RATIO),
                           getIntrinsicHeight() - (int) (getIntrinsicHeight() * TEXT_MARGIN_RATIO));
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

}
