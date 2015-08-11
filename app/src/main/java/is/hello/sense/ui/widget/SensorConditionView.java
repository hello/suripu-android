package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawables;

public class SensorConditionView extends View {
    public static final int ROTATE_START_DELAY_MS = 30;
    public static final int ROTATE_DURATION_MS = 1000;

    private final ValueAnimator rotateAnimator;
    private final Resources resources;

    private boolean rotating = false;
    private float rotateDegrees = 0f;

    private int tintColor;
    private @Nullable Drawable fill;
    private @Nullable Drawable transitionFill;
    private @Nullable Drawable icon;

    private @Nullable AnimatorContext animatorContext;
    private @Nullable ValueAnimator crossFade;


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
        rotateAnimator.setStartDelay(ROTATE_START_DELAY_MS);
        rotateAnimator.setDuration(ROTATE_DURATION_MS);
        rotateAnimator.addUpdateListener(a -> {
            this.rotateDegrees = (float) a.getAnimatedValue();
            invalidate();
        });

        this.resources = getResources();

        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SensorConditionView, defStyleAttr, 0);
            setIcon(attributes.getDrawable(R.styleable.SensorConditionView_senseSensorIcon));
            attributes.recycle();
        }

        setTint(resources.getColor(R.color.sensor_empty));
        setFill(R.drawable.room_check_sensor_border_empty);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (rotating) {
            if (visibility == VISIBLE) {
                if (!rotateAnimator.isRunning()) {
                    rotateAnimator.start();
                }
            } else {
                rotateAnimator.cancel();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        rotateAnimator.cancel();

        super.onDetachedFromWindow();
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        int canvasWidth = canvas.getWidth(),
            canvasHeight = canvas.getHeight();
        int canvasMidX = canvasWidth / 2,
            canvasMidY = canvasHeight / 2;

        if (rotateDegrees > 0f) {
            canvas.save();
            canvas.rotate(rotateDegrees, canvasMidX, canvasMidY);
        }

        if (fill != null) {
            int fillMidX = fill.getIntrinsicWidth() / 2;
            fill.setBounds(canvasMidX - fillMidX, 0, canvasMidX + fillMidX, canvasHeight);
            fill.draw(canvas);
        }

        if (transitionFill != null) {
            int transitionFillMidX = transitionFill.getIntrinsicWidth() / 2;
            transitionFill.setBounds(canvasMidX - transitionFillMidX, 0, canvasMidX + transitionFillMidX, canvasHeight);
            transitionFill.draw(canvas);
        }

        if (rotateDegrees > 0f) {
            canvas.restore();
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

        int specWidthMode = MeasureSpec.getMode(widthMeasureSpec),
            specHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        int specWidth = MeasureSpec.getSize(widthMeasureSpec),
            specHeight = MeasureSpec.getSize(heightMeasureSpec);

        int measuredWidth,
            measuredHeight;
        if (specWidthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(intrinsicWidth, specWidth);
        } else {
            measuredWidth = specWidth;
        }

        if (specHeightMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(intrinsicHeight, specHeight);
        } else {
            measuredHeight = specHeight;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    //endregion


    //region Contents

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setTint(int color) {
        this.tintColor = color;

        if (icon != null) {
            Drawables.setTintColor(icon, color);
        }

        if (fill != null) {
            Drawables.setTintColor(fill, color);
        }

        if (transitionFill != null) {
            Drawables.setTintColor(transitionFill, color);
        }

        invalidate();
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
            setMinimumWidth(fill.getIntrinsicWidth());
            fill.setCallback(this);
            Drawables.setTintColor(fill, tintColor);
        }

        invalidate();
    }

    public void setFill(@DrawableRes int fillRes) {
        setFill(ResourcesCompat.getDrawable(resources, fillRes, null));
    }

    public void crossFadeToFill(@NonNull Drawable newFill, boolean rotate, @Nullable Runnable onCompletion) {
        this.transitionFill = newFill;
        Drawables.setTintColor(newFill, tintColor);
        newFill.setAlpha(0);

        this.crossFade = ValueAnimator.ofFloat(0f, 1f);
        crossFade.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        crossFade.setDuration(Anime.DURATION_SLOW);

        Drawable oldFill = fill;
        if (oldFill == null) {
            throw new IllegalStateException("Cannot transition from nothing");
        }

        crossFade.addUpdateListener(a -> {
            float fraction = a.getAnimatedFraction();
            oldFill.setAlpha(Math.round(255f * (1f - fraction)));
            newFill.setAlpha(Math.round(255f * fraction));
        });

        crossFade.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (rotate) {
                    startRotating();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                oldFill.setAlpha(0);
                newFill.setAlpha(255);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setFill(newFill);
                SensorConditionView.this.transitionFill = null;
                SensorConditionView.this.crossFade = null;

                if (!rotate) {
                    stopRotation();
                }

                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        });

        if (animatorContext != null) {
            crossFade.addListener(animatorContext);
        }

        crossFade.start();
    }

    public void crossFadeToFill(@DrawableRes int fillRes, boolean rotate, @Nullable Runnable onCompletion) {
        crossFadeToFill(ResourcesCompat.getDrawable(resources, fillRes, null), rotate, onCompletion);
    }

    public void stopAnimating() {
        stopRotation();

        if (crossFade != null) {
            crossFade.cancel();
        }
    }

    //endregion


    //region Animations

    public void startRotating() {
        rotateAnimator.start();
        this.rotating = true;
    }

    public void stopRotation() {
        this.rotateDegrees = 0f;
        rotateAnimator.cancel();
        this.rotating = false;
    }

    //endregion

}
