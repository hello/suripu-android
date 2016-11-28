package is.hello.sense.ui.widget.graphing.drawables;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.Log;

import is.hello.sense.R;

public class SleepScoreDrawable extends Drawable {

    private static final float circleThickness = .025f;
    private static final float textMargin = .4f;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String noDataPlaceHolder;
    private int height = 0;
    private int width = 0;
    final Rect textBounds = new Rect();

    public SleepScoreDrawable(@NonNull final Context context) {
        noDataPlaceHolder = "82";// resources.getString(R.string.missing_data_placeholder);
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.background));
        circlePaint.setColor(ContextCompat.getColor(context, R.color.gray4));
        textPaint.setColor(ContextCompat.getColor(context, R.color.gray4));
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;

    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.drawColor(backgroundPaint.getColor());
        final int centerX = canvas.getWidth() / 2;
        final int centerY = canvas.getHeight() / 2;
        final int radius;
        if (centerX > centerY) {
            radius = centerY;
        } else {
            radius = centerX;
        }
        canvas.drawCircle(centerX, centerY, radius, circlePaint);
        canvas.drawCircle(centerX, centerY, radius - radius * circleThickness, backgroundPaint);
        drawCenter(canvas, textPaint, noDataPlaceHolder);


    }

    @Override
    public void setAlpha(final int alpha) {

    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    private void drawCenter(final Canvas canvas,
                            final Paint paint,
                            final String text) {
        canvas.getClipBounds(textBounds);
        final float cHeight = textBounds.height();
        final float cWidth = textBounds.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), textBounds);
        final float x = cWidth / 2f - textBounds.width() / 2f - textBounds.left;
        final float y = cHeight / 2f + textBounds.height() / 2f - textBounds.bottom;
        canvas.drawText(text, x, y, paint);
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setCorrectTextSize() {
        while (doesTextFit()) {
            textPaint.setTextSize(textPaint.getTextSize() + 1);
        }
        textPaint.setTextSize(textPaint.getTextSize() - 1);
        // textPaint.setTextSize(100);
        final Rect bounds = new Rect();
        textPaint.getTextBounds(noDataPlaceHolder, 0, noDataPlaceHolder.length(), bounds);
        Log.e("---Bounds---", "W/H: " + bounds.width() + " / " + bounds.height() + " vs W/H: " + getIntrinsicWidth() + " / " + getIntrinsicHeight());

    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean doesTextFit() {
        final int height = getIntrinsicHeight() - (int) (getIntrinsicHeight() * textMargin);
        final int width = getIntrinsicWidth() - (int) (getIntrinsicWidth() * textMargin);
        // Log.e("Size", "W/H: " + width + " / " + height);
        final Rect bounds = new Rect();
        textPaint.getTextBounds(noDataPlaceHolder, 0, noDataPlaceHolder.length(), bounds);
        // Log.e("Bounds", "W/H: " + bounds.width() + " / " + bounds.height());

        if (bounds.height() > height) {
            return false;
        }
        if (bounds.width() > width) {
            return false;
        }
        return true;
    }
}
