package is.hello.sense.ui.widget.graphing.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import is.hello.sense.R;

//todo use this with new timeline navigation bar
public class SleepScoreIconDrawable extends Drawable {

    private static final float CIRCLE_THICKNESS_RATIO = .1f;
    private static final float TEXT_MARGIN_RATIO = .5f;

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final String noDataPlaceHolder;
    private final int unselectedColor;
    private final int backgroundColor;
    private final int selectedColor;
    private final int fillColor;

    private int height = 0;
    private int width = 0;
    private String text;


    public SleepScoreIconDrawable(@NonNull final Context context) {
        this.selectedColor = ContextCompat.getColor(context, R.color.blue6);
        this.unselectedColor = ContextCompat.getColor(context, R.color.gray4);
        this.fillColor = ContextCompat.getColor(context, R.color.blue2);
        this.backgroundColor = ContextCompat.getColor(context, R.color.background);
        this.noDataPlaceHolder = context.getResources().getString(R.string.missing_data_placeholder);
        this.backgroundPaint.setColor(this.backgroundColor);
        setIsSelected(false);
        setText(null);
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

    @Override
    public void setAlpha(final int alpha) {

    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
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

    public void setIsSelected(final boolean isSelected) {
        if (isSelected) {
            this.circlePaint.setColor(this.selectedColor);
            this.textPaint.setColor(this.selectedColor);
            this.fillPaint.setColor(this.fillColor);
        } else {
            this.circlePaint.setColor(this.unselectedColor);
            this.textPaint.setColor(this.unselectedColor);
            this.fillPaint.setColor(this.backgroundColor);
        }
    }

    public void setText(@Nullable final String text) {
        if (text == null) {
            this.text = this.noDataPlaceHolder;
        } else {
            this.text = text;
        }
        setCorrectTextSize();
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public void setCorrectTextSize() {
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
}
