package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.widget.ToggleButton;

import java.util.List;

import is.hello.sense.R;

/**
 * This class exists because ToggleButton would not center a drawable + text
 * no matter what I tried. Maybe it can be replaced by a better solution.
 */
public class IconAndTextDrawable extends Drawable {
    private final int normalColor;
    private final int selectedColor;
    private final int iconPadding;

    private final Paint textPaint = new Paint();
    private final Rect textBounds = new Rect();

    private final String text;
    private final Drawable iconDrawable;


    //region Creation

    public IconAndTextDrawable(@NonNull Resources resources, @NonNull Drawable iconDrawable, @NonNull String text) {
        this.normalColor = resources.getColor(R.color.border);
        this.selectedColor = resources.getColor(R.color.light_accent);
        this.iconPadding = resources.getDimensionPixelSize(R.dimen.gap_small);

        this.iconDrawable = iconDrawable;
        this.text = text;

        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);

        setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body_small));
        setTypeface(Typeface.create("sans-serif-medium", 0));
        setColorFilter(normalColor, PorterDuff.Mode.SRC_ATOP);
    }

    public static void replaceBuiltInDrawing(@NonNull ToggleButton toggleButton) {
        Drawable drawable = toggleButton.getCompoundDrawablesRelative()[0];
        String text;
        if (toggleButton.getTransformationMethod() != null) {
            text = toggleButton.getTransformationMethod().getTransformation(toggleButton.getText(), toggleButton).toString();
        } else {
            text = toggleButton.getText().toString();
        }

        toggleButton.setBackground(new IconAndTextDrawable(toggleButton.getResources(), drawable, text));
        toggleButton.setText("");
        toggleButton.setTextOn("");
        toggleButton.setTextOff("");
        toggleButton.setCompoundDrawablesRelative(null, null, null, null);
        toggleButton.setContentDescription(text);
    }

    public static void replaceBuiltInDrawing(@NonNull List<ToggleButton> toggleButtons) {
        for (ToggleButton button : toggleButtons) {
            replaceBuiltInDrawing(button);
        }
    }

    //endregion


    //region Drawing

    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        int midX = width / 2;
        int midY = height / 2;

        int iconWidth = iconDrawable.getIntrinsicWidth();
        int iconMidX = iconWidth / 2;
        int iconHeight = iconDrawable.getIntrinsicHeight();
        int iconMidY = iconHeight / 2;

        float textX = (midX - textBounds.centerX()) + iconMidX + (iconPadding / 2);
        float textY = midY - textBounds.centerY();
        canvas.drawText(text, textX, textY, textPaint);

        int iconRight = (int) textX - (iconPadding / 2);
        int iconLeft = iconRight - iconWidth;
        iconDrawable.setBounds(iconLeft, midY - iconMidY, iconRight, midY + iconMidY);
        iconDrawable.draw(canvas);
    }

    @Override
    protected boolean onStateChange(int[] states) {
        for (int state : states) {
            if (state == android.R.attr.state_checked || state == android.R.attr.state_selected) {
                setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
            } else {
                setColorFilter(normalColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
        return super.onStateChange(states);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        measureText();
    }

    protected void measureText() {
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
    }

    //endregion


    //region Properties


    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
        iconDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
        iconDrawable.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }


    public void setTextSize(float textSize) {
        textPaint.setTextSize(textSize);
        measureText();
        invalidateSelf();
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
        measureText();
        invalidateSelf();
    }

    //endregion
}
