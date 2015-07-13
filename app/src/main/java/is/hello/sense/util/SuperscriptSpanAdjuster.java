package is.hello.sense.util;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * From http://stackoverflow.com/questions/8991606/adjusting-text-alignment-using-spannablestring.
 */
public class SuperscriptSpanAdjuster extends MetricAffectingSpan {
    double ratio = 0.5;

    public SuperscriptSpanAdjuster(double ratio) {
        this.ratio = ratio;
    }

    @Override
    public void updateDrawState(TextPaint paint) {
        paint.baselineShift += (int) (paint.ascent() * ratio);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        paint.baselineShift += (int) (paint.ascent() * ratio);
    }
}
