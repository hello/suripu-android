package is.hello.sense.ui.widget.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;
import android.widget.TextView;

public class Drawing {
    public static final float DARK_MULTIPLIER = .02f;

    /**
     * Updates a {@link TextPaint} object with the parameters of a specified text style.
     * <p>
     * Allocates.
     */
    public static void updateTextPaintFromStyle(@NonNull final TextPaint textPaint,
                                                @NonNull final Context context,
                                                @StyleRes final int styleRes) {

        final TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, styleRes);
        textAppearance.updateDrawState(textPaint);
        textAppearance.updateMeasureState(textPaint);
    }

    /**
     * Estimates the line height that text rendered with a given {@code TextPaint} will have.
     * <p>
     * This method allocates, do not call in {@code #onDraw(Canvas)}.
     *
     * @param paint         The paint to use to calculate the font metrics.
     * @param fixUpBaseline Whether or not the line height should be
     *                      adjusted to include the ascent and descent.
     * @return The estimated height of a line rendered with {@code paint}.
     */
    public static int getEstimatedLineHeight(@NonNull final TextPaint paint,
                                             final boolean fixUpBaseline) {
        final Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        final int lineHeight = Math.abs(fontMetrics.bottom - fontMetrics.top);
        if (fixUpBaseline) {
            return (lineHeight +
                    fontMetrics.ascent +
                    fontMetrics.descent);
        } else {
            return lineHeight;
        }
    }

    /**
     * Returns the estimated maximum glyph width for a given paint's text configuration.
     */
    public static int getMaximumGlyphWidth(@NonNull final Paint paint) {
        // 'W' is the widest glyph in Roboto.
        return (int) Math.ceil(paint.measureText("W", 0, 1));
    }

    public static int colorWithAlpha(final int color,
                                     final int newAlpha) {
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int darkenColorBy(final int color,
                                    final float delta) {
        final int alpha = Color.alpha(color);
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= (1f - delta);
        return Color.HSVToColor(alpha, hsv);
    }

    public static void setLetterSpacing(@NonNull final TextView textView,
                                        final float letterSpacing) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textView.setLetterSpacing(letterSpacing);
        }
    }
}
