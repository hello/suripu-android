package is.hello.sense.util.markup.text;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class SerializableStyleSpan extends MetricAffectingSpan implements SerializableSpan {
    private final int style;

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
        Typeface newTypeface = Typeface.create(oldTypeface, style);
        textPaint.setTypeface(newTypeface);
    }
}
