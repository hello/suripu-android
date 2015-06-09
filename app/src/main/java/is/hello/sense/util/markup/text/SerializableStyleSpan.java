package is.hello.sense.util.markup.text;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;

import java.util.ArrayList;
import java.util.List;

public class SerializableStyleSpan extends MetricAffectingSpan implements SerializableSpan {
    private final int style;

    public static String styleToString(int style) {
        if (style == Typeface.NORMAL) {
            return "NORMAL";
        }

        List<String> flags = new ArrayList<>();

        if ((style & Typeface.BOLD) == Typeface.BOLD) {
            flags.add("BOLD");
        }

        if ((style & Typeface.ITALIC) == Typeface.ITALIC) {
            flags.add("ITALIC");
        }

        return TextUtils.join(" | ", flags);
    }

    public SerializableStyleSpan(int style) {
        this.style = style;
    }


    @Override
    public void updateMeasureState(TextPaint textPaint) {
        applyStyle(textPaint);
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        applyStyle(textPaint);
    }


    private void applyStyle(@NonNull TextPaint textPaint) {
        Typeface oldTypeface = textPaint.getTypeface();

        int requestedStyle;
        Typeface newTypeface;
        if (oldTypeface == null) {
            requestedStyle = style;
            newTypeface = Typeface.defaultFromStyle(requestedStyle);
        } else {
            requestedStyle = oldTypeface.getStyle() | style;
            newTypeface = Typeface.create(oldTypeface, requestedStyle);
        }

        int missing = requestedStyle & ~newTypeface.getStyle();
        if ((missing & Typeface.BOLD) == Typeface.BOLD) {
            textPaint.setFakeBoldText(true);
        }
        if ((missing & Typeface.ITALIC) == Typeface.ITALIC) {
            textPaint.setTextSkewX(-0.25f);
        }

        textPaint.setTypeface(newTypeface);
    }


    @Override
    public String toString() {
        return "SerializableStyleSpan{" +
                "style=" + styleToString(style) +
                '}';
    }
}
