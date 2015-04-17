package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

public class SensorTickerView extends LinearLayout {
    private static final int NUMBER_DIGITS = 3;

    private final DigitRotaryView[] digits = new DigitRotaryView[NUMBER_DIGITS];
    private final TextView unitText;

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

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < NUMBER_DIGITS; i++) {
            DigitRotaryView digit = new DigitRotaryView(context);
            digit.setVisibility(INVISIBLE);
            addView(digit, layoutParams);
            digits[i] = digit;
        }

        this.unitText = new TextView(context);
        unitText.setTextAppearance(context, DigitRotaryView.TEXT_APPEARANCE);
        unitText.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0.4f * unitText.getTextSize());
        unitText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        addView(unitText, layoutParams);
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

        int digitsCount = valueString.length();
        for (int i = 0, size = digits.length; i < size; i++) {
            DigitRotaryView digit = digits[i];
            if (i < digitsCount) {
                digit.setVisibility(VISIBLE);
            } else {
                digit.setVisibility(GONE);
            }
            digit.setOnScreenDigit(0);
        }

        int digitIndex = digitsCount - 1;

        int rotations = 0;
        if (digitsCount > 1) {
            rotations = Integer.valueOf(valueString.substring(0, digitIndex), 10);
        }

        if (animatorContext != null) {
            animatorContext.beginAnimation();
        }
        int targetDigit = Integer.valueOf(valueString.substring(digitIndex, digitIndex + 1), 10);

        DigitRotaryView digitView = digits[digitIndex];
        DigitRotaryView.Spin spin = digitView.createSpin(targetDigit, rotations, 1000);
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
        DigitRotaryView digit = digits[digitIndex];
        if (digit.getOffScreenDigit() == 0 && digitIndex >= 0) {
            incrementAdjacent(spinDuration, digitIndex - 1);
        }
        digit.spinToNextDigit(spinDuration, null);
    }

    //endregion
}
