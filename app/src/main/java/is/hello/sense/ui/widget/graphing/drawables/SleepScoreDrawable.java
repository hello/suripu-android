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

import is.hello.sense.R;

public class SleepScoreDrawable extends Drawable {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final int circleThickness;
    private String noDataPlaceHolder;

    public SleepScoreDrawable(@NonNull final Context context) {
        final Resources resources = context.getResources();
        noDataPlaceHolder = resources.getString(R.string.missing_data_placeholder);
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.background));
        circlePaint.setColor(ContextCompat.getColor(context, R.color.gray4));
        textPaint.setColor(ContextCompat.getColor(context, R.color.gray4));
        circleThickness = 10; // todo use a resource.


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
        canvas.drawCircle(centerX, centerY, radius - circleThickness, backgroundPaint);
        canvas.drawText(noDataPlaceHolder, centerX, centerY, textPaint);


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

    private void setCorrectTextSize() {
        while (doesTextFit()) {
            textPaint.setTextSize(textPaint.getTextSize() + 1);
        }
        textPaint.setTextSize(textPaint.getTextSize() - 1);

    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean doesTextFit() {
        final int height = getIntrinsicHeight();
        final int width = getIntrinsicWidth();
        final Rect bounds = new Rect();
        textPaint.getTextBounds(noDataPlaceHolder, 0, noDataPlaceHolder.length(), bounds);

        if (bounds.height() > height) {
            return false;
        }
        if (bounds.width() > width) {
            return false;
        }
        return true;
    }
}
