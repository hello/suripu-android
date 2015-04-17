package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import is.hello.sense.R;
import rx.functions.Action1;

public class DigitRotaryView extends View implements ValueAnimator.AnimatorUpdateListener {
    //region Constants

    public static final long MIN_SPIN_DURATION_MS = 20;
    public static final @StyleRes int TEXT_APPEARANCE = R.style.AppTheme_Text_BigScore;

    //endregion


    //region Drawing

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final int itemWidth;
    private final int itemHeight;
    private final int textYFixUp;

    //endregion


    //region Items

    private final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private int onScreenDigit = 0;
    private int offScreenDigit = 1;

    //endregion


    //region Animation

    private final ValueAnimator offsetAnimator;
    private float digitsOffset = 0f;

    //endregion


    //region Lifecycle

    public DigitRotaryView(@NonNull Context context) {
        this(context, null);
    }

    public DigitRotaryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DigitRotaryView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, TEXT_APPEARANCE);
        textAppearance.updateMeasureState(textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect textBounds = new Rect();
        textPaint.getTextBounds(DIGITS, 4, 1, textBounds); // '4' is the widest number in Roboto
        this.itemWidth = textBounds.width();

        Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();
        this.itemHeight = Math.abs(fontMetrics.bottom - fontMetrics.top);
        this.textYFixUp = fontMetrics.bottom;

        this.offsetAnimator = ValueAnimator.ofFloat(0f, 1f);
        offsetAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        offsetAnimator.addUpdateListener(this);
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        int canvasWidth = canvas.getWidth(),
                canvasHeight = canvas.getHeight();

        float midX = canvasWidth / 2f;
        float offsetY = (canvasHeight * digitsOffset);

        canvas.drawText(DIGITS, onScreenDigit, 1, midX, canvasHeight - textYFixUp + offsetY, textPaint);

        if (digitsOffset > 0f) {
            canvas.drawText(DIGITS, offScreenDigit, 1, midX, offsetY - textYFixUp, textPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int suggestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY: {
                measuredWidth = suggestedWidth;
                break;
            }
            case MeasureSpec.AT_MOST: {
                measuredWidth = Math.min(itemWidth, suggestedWidth);
                break;
            }
            default:
            case MeasureSpec.UNSPECIFIED: {
                measuredWidth = getSuggestedMinimumWidth();
                break;
            }
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int suggestedHeight = MeasureSpec.getSize(heightMeasureSpec);
        int measuredHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY: {
                measuredHeight = suggestedHeight;
                break;
            }
            case MeasureSpec.AT_MOST: {
                measuredHeight = Math.min(itemHeight, suggestedHeight);
                break;
            }
            default:
            case MeasureSpec.UNSPECIFIED: {
                measuredHeight = getSuggestedMinimumHeight();
                break;
            }
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    //endregion


    //region Attributes

    public void setOnScreenDigit(int onScreenDigit) {
        this.digitsOffset = 0f;
        this.onScreenDigit = onScreenDigit;

        if (onScreenDigit < DIGITS.length - 1) {
            this.offScreenDigit = onScreenDigit + 1;
        } else {
            this.offScreenDigit = 0;
        }

        invalidate();
    }

    public int getOnScreenDigit() {
        return onScreenDigit;
    }

    public void setTextColor(int textColor) {
        textPaint.setColor(textColor);
        invalidate();
    }

    //endregion


    //region Animation

    private void prepareForOffsetAnimation() {
        offsetAnimator.cancel();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        this.digitsOffset = animation.getAnimatedFraction();
        invalidate();
    }

    public void spinToNextDigit(long duration, @NonNull Action1<Boolean> onCompletion) {
        prepareForOffsetAnimation();

        offsetAnimator.setDuration(duration);
        offsetAnimator.addListener(new AnimatorListenerAdapter() {
            boolean wasCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeListener(this);

                if (!wasCanceled) {
                    setOnScreenDigit(offScreenDigit);
                }

                // Re-using the ValueAnimator means we can't
                // call this on the same looper callback.
                post(() -> onCompletion.call(!wasCanceled));
            }
        });
        offsetAnimator.start();
    }

    public Spin createSpin(int targetDigit, int rotations, long targetDuration) {
        return new Spin(DIGITS.length, targetDigit, rotations, targetDuration);
    }

    public void runSpin(@NonNull Spin spin,
                        @NonNull Action1<Integer> onRotation,
                        @NonNull Action1<Boolean> onCompletion) {
        setOnScreenDigit(0);
        spinToNextDigit(spin.singleSpinDuration, new Action1<Boolean>() {
            final int repetitionRollover = DIGITS.length;
            int repetitions = 0, rotationCount = 0;

            @Override
            public void call(Boolean completed) {
                if (!completed) {
                    onCompletion.call(false);
                    return;
                }

                this.repetitions++;
                if (repetitions >= repetitionRollover) {
                    onRotation.call(rotationCount);

                    this.repetitions = 0;
                    this.rotationCount++;
                }

                if (rotationCount < spin.rotations || onScreenDigit < spin.targetDigit) {
                    spinToNextDigit(spin.singleSpinDuration, this);
                } else {
                    onCompletion.call(true);
                }
            }
        });
    }

    public void stopAnimating() {
        offsetAnimator.cancel();
    }

    //endregion


    public static class Spin {
        public final int targetDigit;
        public final int rotations;

        public final long singleSpinDuration;
        public final long totalDuration;

        public Spin(int totalDigits, int targetDigit, int rotations, long targetDuration) {
            this.targetDigit = targetDigit;
            this.rotations = rotations;

            int digitsShown = (totalDigits * rotations) + targetDigit;
            this.singleSpinDuration = Math.max(MIN_SPIN_DURATION_MS, targetDuration / digitsShown);
            this.totalDuration = singleSpinDuration * digitsShown;
        }
    }
}
