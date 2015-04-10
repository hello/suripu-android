package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.widget.util.Drawables;

public class SensorConditionView extends View {
    private final ValueAnimator rotateAnimator;
    private final List<Animator> runningAnimators = new ArrayList<>();

    private float rotateAmount = 0f;
    private int tintColor;
    private @Nullable Drawable fill;
    private @Nullable Drawable icon;

    //region Lifecycle

    public SensorConditionView(@NonNull Context context) {
        this(context, null);
    }

    public SensorConditionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorConditionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.rotateAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotateAnimator.setRepeatMode(ValueAnimator.RESTART);
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.setStartDelay(30);
        rotateAnimator.setDuration(1000);
        rotateAnimator.addUpdateListener(a -> {
            this.rotateAmount = (float) a.getAnimatedValue();
            invalidate();
        });

        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SensorConditionView, defStyleAttr, 0);
            setIcon(attributes.getDrawable(R.styleable.SensorConditionView_senseSensorIcon));
            attributes.recycle();
        }

        setEmptyState();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility == VISIBLE) {
            for (Animator animator : runningAnimators) {
                if (!animator.isRunning()) {
                    animator.start();
                }
            }
        } else {
            for (Animator animator : runningAnimators) {
                animator.cancel();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        for (Animator animator : runningAnimators) {
            animator.cancel();
        }
        runningAnimators.clear();

        super.onDetachedFromWindow();
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth(),
            height = canvas.getHeight();
            int canvasMidX = canvas.getWidth() / 2,
                canvasMidY = canvas.getHeight() / 2;

        if (fill != null) {
            if (rotateAmount >= 0f) {
                canvas.save();
                canvas.rotate(rotateAmount, canvasMidX, canvasMidY);
            }

            fill.setBounds(0, 0, width, height);
            fill.draw(canvas);

            if (rotateAmount >= 0f) {
                canvas.restore();
            }
        }

        if (icon != null) {

            int iconMidX = icon.getIntrinsicWidth() / 2;
            int iconMidY = icon.getIntrinsicHeight() / 2;

            icon.setBounds(canvasMidX - iconMidX, canvasMidY - iconMidY,
                           canvasMidX + iconMidX, canvasMidY + iconMidY);
            icon.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int intrinsicWidth = 0,
            intrinsicHeight = 0;
        if (fill != null) {
            intrinsicWidth = fill.getIntrinsicWidth();
            intrinsicHeight = fill.getIntrinsicHeight();
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec),
            heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec),
            height = MeasureSpec.getSize(heightMeasureSpec);

        int measuredWidth,
            measuredHeight;
        if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(intrinsicWidth, width);
        } else {
            measuredWidth = width;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(intrinsicHeight, height);
        } else {
            measuredHeight = height;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    //endregion


    //region Contents

    public void setTint(int color) {
        this.tintColor = color;

        if (icon != null) {
            Drawables.setTintColor(icon, color);
        }

        if (fill != null) {
            Drawables.setTintColor(fill, color);
        }

        invalidate();
    }

    public void animateToTint(int newColor, @Nullable Runnable onCompletion) {
        ValueAnimator animator = ValueAnimator.ofInt(tintColor, newColor);
        animator.setEvaluator(new ArgbEvaluator());
        AnimatorConfig.DEFAULT.apply(animator);
        animator.addUpdateListener(a -> {
            setTint((int) a.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                runningAnimators.remove(animation);
                setTint(newColor);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                runningAnimators.remove(animation);
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                runningAnimators.add(animation);
            }
        });
        animator.start();
    }

    public void setIcon(@Nullable Drawable icon) {
        if (this.icon != null) {
            this.icon.setCallback(null);
            this.icon = null;
        }

        if (icon != null) {
            this.icon = icon.mutate();
            this.icon.setCallback(this);
            Drawables.setTintColor(icon, tintColor);
        }

        invalidate();
    }

    public void setFill(@Nullable Drawable fill) {
        if (this.fill != null) {
            this.fill.setCallback(null);
            this.fill = null;
        }

        if (fill != null) {
            this.fill = fill.mutate();
            fill.setCallback(this);
            Drawables.setTintColor(fill, tintColor);
        }

        invalidate();
    }

    public void setFill(@DrawableRes int fillRes) {
        setFill(getResources().getDrawable(fillRes));
    }

    //endregion


    //region Animations

    public void startRotating() {
        rotateAnimator.start();
        runningAnimators.add(rotateAnimator);
    }

    public void stopRotation() {
        this.rotateAmount = 0f;
        rotateAnimator.cancel();
        runningAnimators.remove(rotateAnimator);
    }

    //endregion


    //region Modes

    public void setEmptyState() {
        stopRotation();
        setTint(getResources().getColor(R.color.sensor_empty));
        setFill(R.drawable.room_check_sensor_border_empty);
    }

    public void setProgressState() {
        animateToTint(getResources().getColor(R.color.light_accent), () -> {
            setFill(R.drawable.room_check_sensor_border_loading);
            startRotating();
        });
    }

    public void setCompletedState(@NonNull Condition condition) {
        stopRotation();
        animateToTint(getResources().getColor(condition.colorRes), null);
        setFill(R.drawable.room_check_sensor_border_filled);
    }

    //endregion
}
