package is.hello.sense.flows.expansions.ui.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import is.hello.sense.R;
import is.hello.sense.util.Constants;

import static is.hello.sense.util.PaintUtil.drawAndCenterText;

/**
 * Will render text with a border around it.
 */
public class ExpansionTextDrawable extends Drawable {
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int radius;
    private final int borderWidth;

    private String text = Constants.EMPTY_STRING;
    private int height = 0;
    private int width = 0;

    public ExpansionTextDrawable(@NonNull final Context context) {
        this.textPaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.text_h5));
        this.textPaint.setColor(ContextCompat.getColor(context, R.color.gray5));
        this.borderPaint.setColor(ContextCompat.getColor(context, R.color.gray2));
        this.backgroundPaint.setColor(ContextCompat.getColor(context, R.color.gray1));
        this.borderWidth = context.getResources().getDimensionPixelSize(R.dimen.divider_size_thick);
        this.radius = context.getResources().getDimensionPixelSize(R.dimen.x_5);
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
    public void draw(@NonNull final Canvas canvas) {
        final Path path = new Path();
        final RectF rectF = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());

        path.addRoundRect(rectF,
                          this.radius,
                          this.radius,
                          Path.Direction.CW);
        canvas.drawPath(path,
                        this.borderPaint);
        rectF.inset(this.borderWidth,
                    this.borderWidth);
        path.reset();
        path.addRoundRect(rectF,
                          this.radius,
                          this.radius,
                          Path.Direction.CW);
        canvas.drawPath(path,
                        this.backgroundPaint);
        drawAndCenterText(canvas,
                          this.textPaint,
                          this.text);
    }

    @Override
    public void setAlpha(final int i) {
        this.textPaint.setAlpha(i);
        this.borderPaint.setAlpha(i);
        this.backgroundPaint.setAlpha(i);

    }

    /**
     * this does nothing.
     * @param colorFilter
     */
    @Override
    public void setColorFilter(final ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    public void setDimensions(final int width,
                              final int height) {
        this.width = width;
        this.height = height;
    }

    public void setText(@Nullable final String text) {
        if (text == null) {
            this.text = Constants.EMPTY_STRING;
            return;
        }
        this.text = text;
    }

}
