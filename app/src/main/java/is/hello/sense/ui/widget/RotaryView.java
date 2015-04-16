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

public class RotaryView extends View {
    //region Constants

    public static final int NO_ITEM = -1;

    public static final @StyleRes int TEXT_APPEARANCE = R.style.AppTheme_Text_BigScore;

    //endregion


    //region Drawing

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final int itemWidth;
    private final int itemHeight;
    private final int textYFixUp;

    //endregion


    //region Items

    private @Nullable String[] items;
    private int onScreenItem = NO_ITEM;
    private int offScreenItem = NO_ITEM;

    //endregion


    //region Animation

    private final ValueAnimator offsetAnimator;
    private float itemsOffset = 0f;

    //endregion


    //region Lifecycle

    public RotaryView(@NonNull Context context) {
        this(context, null);
    }

    public RotaryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotaryView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TextAppearanceSpan textAppearance = new TextAppearanceSpan(context, TEXT_APPEARANCE);
        textAppearance.updateMeasureState(textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect textBounds = new Rect();
        textPaint.getTextBounds("4", 0, 1, textBounds); // widest number in Roboto
        this.itemWidth = textBounds.width();

        Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();
        this.itemHeight = Math.abs(fontMetrics.bottom - fontMetrics.top);
        this.textYFixUp = fontMetrics.bottom;

        this.offsetAnimator = ValueAnimator.ofFloat(0f, 1f);
        offsetAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        offsetAnimator.addUpdateListener(a -> {
            this.itemsOffset = a.getAnimatedFraction();
            invalidate();
        });
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        if (items == null || onScreenItem == NO_ITEM) {
            return;
        }

        int canvasWidth = canvas.getWidth(),
            canvasHeight = canvas.getHeight();

        float midX = canvasWidth / 2f;
        float offsetY = (canvasHeight * itemsOffset);

        String visible = items[onScreenItem];
        canvas.drawText(visible, midX, canvasHeight - textYFixUp + offsetY, textPaint);

        if (offScreenItem != NO_ITEM && itemsOffset > 0f) {
            String upcoming = items[offScreenItem];
            canvas.drawText(upcoming, midX, offsetY - textYFixUp, textPaint);
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

    public void setOnScreenItem(int onScreenItem) {
        this.itemsOffset = 0f;
        this.onScreenItem = onScreenItem;

        if (items != null && onScreenItem != NO_ITEM) {
            if (onScreenItem < items.length - 1) {
                this.offScreenItem = onScreenItem + 1;
            } else {
                this.offScreenItem = 0;
            }
        } else {
            this.offScreenItem = NO_ITEM;
        }

        invalidate();
    }

    public int getOnScreenItem() {
        return onScreenItem;
    }

    public void setItems(@NonNull String[] items) {
        this.items = items;

        if (items.length == 0) {
            setOnScreenItem(NO_ITEM);
        } else {
            setOnScreenItem(0);
        }
    }

    public void setTextColor(int textColor) {
        textPaint.setColor(textColor);
    }

    //endregion


    //region Animation

    private void prepareForOffsetAnimation() {
        offsetAnimator.cancel();
    }

    public void spinToNext(long duration, @NonNull Action1<Boolean> onCompletion) {
        prepareForOffsetAnimation();

        offsetAnimator.setDuration(duration);
        offsetAnimator.addListener(new AnimatorListenerAdapter() {
            boolean wasCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.wasCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeListener(this);

                if (!wasCanceled) {
                    setOnScreenItem(offScreenItem);
                }

                // Re-using the ValueAnimator means we can't
                // call this on the same looper callback.
                post(() -> onCompletion.call(!wasCanceled));
            }
        });
        offsetAnimator.start();
    }

    public Spin createSpin(int targetItem, int rotations, long targetDuration) {
        if (items == null) {
            throw new IllegalStateException("Cannot create spin without items");
        }

        return new Spin(items.length, targetItem, rotations, targetDuration);
    }

    public void runSpin(@NonNull Spin spin,
                        @NonNull Action1<Integer> onRotation,
                        @NonNull Action1<Boolean> onCompletion) {
        if (items == null) {
            onCompletion.call(false);
            return;
        }

        setOnScreenItem(0);
        spinToNext(spin.singleSpinDuration, new Action1<Boolean>() {
            final int repetitionRollover = items.length;
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

                if (rotationCount < spin.rotations || onScreenItem < spin.targetItem) {
                    spinToNext(spin.singleSpinDuration, this);
                } else {
                    onCompletion.call(true);
                }
            }
        });
    }

    //endregion


    public static class Spin {
        public final int targetItem;
        public final int rotations;

        public final long singleSpinDuration;
        public final long totalDuration;

        public Spin(int itemCount, int targetItem, int rotations, long targetDuration) {
            this.targetItem = targetItem;
            this.rotations = rotations;

            int itemsShown = (itemCount * rotations) + targetItem;
            this.singleSpinDuration = Math.max(20, targetDuration / itemsShown);
            this.totalDuration = singleSpinDuration * itemsShown;
        }
    }
}
