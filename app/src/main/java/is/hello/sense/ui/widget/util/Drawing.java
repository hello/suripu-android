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
    /**
     * Updates a {@link TextPaint} object with the parameters of a specified text style.
     * <p/>
     * Allocates.
     */
    public static void updateTextPaintFromStyle(@NonNull TextPaint textPaint,
                                                @NonNull Context context,
                                                @StyleRes int styleRes) {
        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, styleRes);
        textAppearance.updateDrawState(textPaint);
        textAppearance.updateMeasureState(textPaint);
    }

    /**
     * Returns the estimated height that text rendered with a given paint will have.
     * <p>
     * This method allocates.
     */
    public static int getEstimatedTextHeight(@NonNull Paint paint) {
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        return Math.abs(fontMetrics.bottom - fontMetrics.top);
    }

    /**
     * Returns the estimated maximum glyph width for a given paint's text configuration.
     */
    public static int getMaximumGlyphWidth(@NonNull Paint paint) {
        // 'W' is the widest glyph in Roboto.
        return (int) Math.ceil(paint.measureText("W", 0, 1));
    }

    public static int colorWithAlpha(int color, int newAlpha) {
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int darkenColorBy(int color, float delta) {
        int alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= (1f - delta);
        return Color.HSVToColor(alpha, hsv);
    }

    public static void setLetterSpacing(@NonNull TextView textView,
                                        float letterSpacing) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textView.setLetterSpacing(letterSpacing);
        }
    }
}
