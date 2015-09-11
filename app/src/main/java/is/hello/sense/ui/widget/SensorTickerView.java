package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.util.Logger;
import is.hello.sense.util.NotLocalizable;
import rx.functions.Action1;

@NotLocalizable({
        NotLocalizable.BecauseOf.RTL,
        NotLocalizable.BecauseOf.ALPHABET,
})
public class SensorTickerView extends LinearLayout {
    private static final int NUMBER_DIGITS = 3;

    private final DigitRotaryView[] digits = new DigitRotaryView[NUMBER_DIGITS];
    private final TextView unitText;
    private final GradientDrawable fadeGradient;
    private final int fadeGradientHeight;

    private @Nullable AnimatorContext animatorContext;

    //region Lifecycle

    public SensorTickerView(@NonNull Context context) {
        this(context, null);
    }

    public SensorTickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorTickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        final Resources resources = getResources();

        final int letterSpacing = resources.getDimensionPixelSize(R.dimen.view_sensor_ticker_letter_spacing);
        final DigitRotaryView.RenderInfo renderInfo = new DigitRotaryView.RenderInfo(context);
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                           LayoutParams.WRAP_CONTENT);
        layoutParams.rightMargin = letterSpacing;
        for (int i = 0; i < NUMBER_DIGITS; i++) {
            final DigitRotaryView digit = new DigitRotaryView(context, renderInfo);
            digit.setVisibility(INVISIBLE);
            addView(digit, layoutParams);
            digits[i] = digit;
        }

        this.unitText = new TextView(context);
        unitText.setTextAppearance(context, DigitRotaryView.TEXT_APPEARANCE);
        unitText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0.35f * unitText.getTextSize());
        unitText.setSingleLine(true);
        unitText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        unitText.setIncludeFontPadding(false);
        unitText.setPadding(0, renderInfo.textYFixUp, 0, 0);
        addView(unitText, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        final int[] colors = { Color.WHITE, Color.TRANSPARENT };
        this.fadeGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        this.fadeGradientHeight = resources.getDimensionPixelSize(R.dimen.view_sensor_ticker_fade_height);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        final int width = canvas.getWidth(),
                  height = canvas.getHeight();

        fadeGradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        fadeGradient.setBounds(0, 0, width, fadeGradientHeight);
        fadeGradient.draw(canvas);

        fadeGradient.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
        fadeGradient.setBounds(0, height - fadeGradientHeight, width, height);
        fadeGradient.draw(canvas);
    }

    //endregion


    //region Values

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setTextColor(int color) {
        unitText.setTextColor(color);
        for (DigitRotaryView digit : digits) {
            digit.setTextColor(color);
        }
    }

    public void stopAnimating() {
        for (DigitRotaryView digit : digits) {
            digit.stopAnimating();
        }
    }

    public long animateToValue(int value, @Nullable String unit, @NonNull Action1<Boolean> onCompletion) {
        unitText.setText(unit);

        String valueString = Integer.toString(value);
        if (valueString.length() > NUMBER_DIGITS) {
            Logger.warn(getClass().getSimpleName(), "Truncating ticker value '" + value + "'");
            valueString = valueString.substring(0, NUMBER_DIGITS);
        }

        final int digitsCount = valueString.length();
        for (int i = 0, size = digits.length; i < size; i++) {
            final DigitRotaryView digit = digits[i];
            if (i < digitsCount) {
                digit.setVisibility(VISIBLE);
            } else {
                digit.setVisibility(GONE);
            }
            digit.setOnScreenDigit(0);
        }

        final int digitIndex = digitsCount - 1;

        int rotations = 0;
        if (digitsCount > 1) {
            rotations = Integer.valueOf(valueString.substring(0, digitIndex), 10);
        }

        if (animatorContext != null) {
            animatorContext.beginAnimation();
        }
        final int targetDigit = Integer.valueOf(valueString.substring(digitIndex, digitIndex + 1), 10);

        final DigitRotaryView digitView = digits[digitIndex];
        final DigitRotaryView.Spin spin = digitView.createSpin(targetDigit, rotations, 1000);
        digitView.runSpin(spin, rotation -> {
            incrementAdjacent(spin.adjacentDuration, digitIndex - 1);
        }, finished -> {
            if (animatorContext != null) {
                animatorContext.endAnimation();
            }
            onCompletion.call(finished);
        });

        return spin.totalDuration;
    }

    private void incrementAdjacent(long spinDuration, int digitIndex) {
        final DigitRotaryView digit = digits[digitIndex];
        if (digit.getOffScreenDigit() == 0 && digitIndex >= 0) {
            incrementAdjacent(spinDuration, digitIndex - 1);
        }
        digit.spinToNextDigit(spinDuration, null);
    }

    //endregion
}
