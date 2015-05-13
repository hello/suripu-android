package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;

public class TextDrawable extends Drawable {
    private final Context context;
    private final TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | TextPaint.SUBPIXEL_TEXT_FLAG);
    private final Rect textBounds = new Rect();

    private float lineSpacingMultiple = 1f;
    private float lineSpacingAdditional = 0f;

    private @Nullable CharSequence text;
    private @Nullable Layout layout;

    public TextDrawable(@NonNull Context context) {
        this.context = context;
    }

    public TextDrawable(@NonNull Context context, @StyleRes int textAppearanceRes) {
        this(context);

        setTextAppearance(textAppearanceRes);
    }

    //region Drawing

    @Override
    public void draw(Canvas canvas) {
        if (layout != null) {
            int save = canvas.save();

            Rect bounds = getBounds();
            canvas.translate(bounds.left, bounds.top);

            layout.draw(canvas);

            canvas.restoreToCount(save);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (textBounds.width() != bounds.width() || textBounds.height() != bounds.height()) {
            measure();
        }
    }

    private void measure() {
        if (TextUtils.isEmpty(text)) {
            this.layout = null;
            textBounds.setEmpty();
        } else {
            int boundsWidth = getBounds().width();
            int desiredWidth = boundsWidth > 0 ? boundsWidth : (int) Layout.getDesiredWidth(text, textPaint);

            BoringLayout.Metrics boringMetrics = BoringLayout.isBoring(text, textPaint);
            if (boringMetrics != null) {
                if (layout instanceof BoringLayout) {
                    this.layout = ((BoringLayout) layout).replaceOrMake(text, textPaint,
                            desiredWidth, Layout.Alignment.ALIGN_NORMAL,
                            lineSpacingMultiple, lineSpacingAdditional,
                            boringMetrics, true);
                } else {
                    this.layout = new BoringLayout(text, textPaint,
                            desiredWidth, Layout.Alignment.ALIGN_NORMAL,
                            lineSpacingMultiple, lineSpacingAdditional,
                            boringMetrics, true);
                }
            } else {
                this.layout = new StaticLayout(text, textPaint,
                        desiredWidth, Layout.Alignment.ALIGN_NORMAL,
                        lineSpacingMultiple, lineSpacingAdditional, true);
            }

            textBounds.set(0, 0, layout.getWidth(), layout.getHeight());
        }

        invalidateSelf();
    }

    //endregion


    //region Attributes

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return textBounds.width();
    }

    @Override
    public int getIntrinsicHeight() {
        return textBounds.height();
    }

    //endregion


    //region Text

    public void setText(@Nullable CharSequence text) {
        this.text = text;
        measure();
    }

    public void setText(@StringRes int textRes) {
        setText(context.getText(textRes));
    }

    public @Nullable CharSequence getText() {
        return text;
    }

    public Typeface setTypeface(Typeface typeface) {
        Typeface newTypeface = textPaint.setTypeface(typeface);
        measure();
        return newTypeface;
    }

    public void setTextSize(float textSize) {
        textPaint.setTextSize(textSize);
        measure();
    }

    public void setLineSpacingMultiple(float lineSpacingMultiple) {
        this.lineSpacingMultiple = lineSpacingMultiple;
        measure();
    }

    public void setLineSpacingAdditional(float lineSpacingAdditional) {
        this.lineSpacingAdditional = lineSpacingAdditional;
        measure();
    }

    public void setTextAppearance(@StyleRes int textAppearanceRes) {
        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, textAppearanceRes);
        textAppearance.updateMeasureState(textPaint);
        textAppearance.updateDrawState(textPaint);
        measure();
    }

    //endregion
}
