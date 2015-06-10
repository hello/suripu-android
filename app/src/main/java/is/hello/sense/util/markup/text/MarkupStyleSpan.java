package is.hello.sense.util.markup.text;

import android.graphics.Typeface;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;

import java.util.ArrayList;
import java.util.List;

public class MarkupStyleSpan extends MetricAffectingSpan implements MarkupSpan {
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

    public MarkupStyleSpan(int style) {
        this.style = style;
    }


    //region Serialization

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(style);
    }

    public static final Creator<MarkupStyleSpan> CREATOR = new Creator<MarkupStyleSpan>() {
        @Override
        public MarkupStyleSpan createFromParcel(Parcel source) {
            return new MarkupStyleSpan(source.readInt());
        }

        @Override
        public MarkupStyleSpan[] newArray(int size) {
            return new MarkupStyleSpan[size];
        }
    };

    //endregion


    //region Rendering

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

    //endregion


    @Override
    public String toString() {
        return "SerializableStyleSpan{" +
                "style=" + styleToString(style) +
                '}';
    }
}
