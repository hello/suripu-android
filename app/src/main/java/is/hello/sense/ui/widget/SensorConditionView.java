package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawables;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SensorConditionView extends FrameLayout {
    private final ProgressBar progressBar;
    private final Resources resources;

    private int tintColor;
    private @Nullable Drawable icon;
    private @Nullable Drawable transitionIcon;

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

        setWillNotDraw(false);

        this.resources = getResources();

        this.progressBar = new ProgressBar(context);
        final Drawable indeterminate = ResourcesCompat.getDrawable(resources,
                                                                   R.drawable.animated_room_check_progress_bar,
                                                                   null);
        progressBar.setIndeterminateDrawable(indeterminate);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(INVISIBLE);

        final int width = resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_view_width);
        final int height = resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_view_height);
        addView(progressBar, new LayoutParams(width, height, Gravity.CENTER));

        setTint(resources.getColor(R.color.sensor_empty));
    }

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (crossFade != null) {
            crossFade.cancel();
        }

        Anime.cancelAll(progressBar);
        progressBar.setVisibility(INVISIBLE);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        int canvasWidth = canvas.getWidth(),
            canvasHeight = canvas.getHeight();
        int canvasMidX = canvasWidth / 2;

        if (icon != null) {
            int iconMidX = icon.getIntrinsicWidth() / 2;
            icon.setBounds(canvasMidX - iconMidX, 0,
                           canvasMidX + iconMidX, canvasHeight);
            icon.draw(canvas);
        }

        if (transitionIcon != null) {
            int iconMidX = transitionIcon.getIntrinsicWidth() / 2;
            transitionIcon.setBounds(canvasMidX - iconMidX, 0,
                                     canvasMidX + iconMidX, canvasHeight);
            transitionIcon.draw(canvas);
        }
    }

    //endregion


    //region Contents

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setTint(int color) {
        this.tintColor = color;

        final Drawable indeterminate = progressBar.getIndeterminateDrawable();
        Drawables.setTintColor(indeterminate, color);

        if (icon != null) {
            Drawables.setTintColor(icon, color);
        }

        if (transitionIcon != null) {
            Drawables.setTintColor(transitionIcon, color);
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
            icon.setCallback(this);
            Drawables.setTintColor(icon, tintColor);
        }

        invalidate();
    }

    public void setIcon(@DrawableRes int fillRes) {
        setIcon(ResourcesCompat.getDrawable(resources, fillRes, null));
    }

    public void transitionToIcon(@NonNull Drawable newIcon, @Nullable Runnable onCompletion) {
        this.transitionIcon = newIcon;
        Drawables.setTintColor(newIcon, tintColor);
        newIcon.setAlpha(0);

        this.crossFade = ValueAnimator.ofFloat(0f, 1f);
        crossFade.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        crossFade.setDuration(Anime.DURATION_SLOW);

        Drawable oldIcon = icon;
        if (oldIcon == null) {
            throw new IllegalStateException("Cannot transition from nothing");
        }

        crossFade.addUpdateListener(a -> {
            float fraction = a.getAnimatedFraction();
            oldIcon.setAlpha(Math.round(255f * (1f - fraction)));
            newIcon.setAlpha(Math.round(255f * fraction));
            invalidate();
        });

        crossFade.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (progressBar.getVisibility() == VISIBLE) {
                    fadeOutProgressIndicator();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                oldIcon.setAlpha(0);
                newIcon.setAlpha(255);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setIcon(newIcon);
                SensorConditionView.this.transitionIcon = null;
                SensorConditionView.this.crossFade = null;

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

    public void transitionToIcon(@DrawableRes int iconRes, @Nullable Runnable onCompletion) {
        transitionToIcon(ResourcesCompat.getDrawable(resources, iconRes, null), onCompletion);
    }

    public void fadeInProgressIndicator(@NonNull Runnable onCompletion) {
        animatorFor(progressBar, animatorContext)
                .fadeIn()
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        onCompletion.run();
                    }
                })
                .start();
    }

    public void fadeOutProgressIndicator() {
        animatorFor(progressBar, animatorContext)
                .fadeOut(INVISIBLE)
                .start();
    }

    //endregion

}
