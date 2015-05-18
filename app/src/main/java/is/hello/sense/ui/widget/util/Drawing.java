package is.hello.sense.ui.widget.util;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

public class Drawing {
    /**
     * Updates a {@link TextPaint} object with the parameters of a specified text style.
     * <p/>
     * Allocates.
     */
    public static void updateTextPaintFromStyle(@NonNull TextPaint textPaint, @NonNull Context context, @StyleRes int styleRes) {
        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, styleRes);
        textAppearance.updateDrawState(textPaint);
        textAppearance.updateMeasureState(textPaint);
    }

    /**
     * Creates a {@link TextPaint} object with the parameters of a specified text style.
     */
    public static @NonNull TextPaint createTextPaintFromStyle(@NonNull Context context, @StyleRes int styleRes) {
        TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        updateTextPaintFromStyle(textPaint, context, styleRes);
        return textPaint;
    }

    /**
     * Returns the estimated height that text rendered with a given paint will have.
     * <p/>
     * This method allocates.
     */
    public static int getEstimatedTextHeight(@NonNull Paint paint) {
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        return Math.abs(fontMetrics.bottom - fontMetrics.top);
    }

    /**
     * {@link android.animation.ArgbEvaluator#evaluate(float, Object, Object)}
     * without the costs of auto-boxing associated with it.
     */
    public static int interpolateColors(float fraction, int startColor, int endColor) {
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return (startA + (int)(fraction * (endA - startA))) << 24 |
                (startR + (int)(fraction * (endR - startR))) << 16 |
                (startG + (int)(fraction * (endG - startG))) << 8 |
                (startB + (int)(fraction * (endB - startB)));
    }

    /**
     * {@link android.animation.FloatEvaluator#evaluate(float, Number, Number)}
     * without the costs of auto-boxing associated with it.
     */
    public static float interpolateFloats(float fraction, float start, float end) {
        return (start + fraction * (end - start));
    }
}
