package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;

public class PanImageView extends View {
    private @Nullable Drawable image;
    private float panAmount = 0f;
    private int scaledImageWidth = 0;

    private @Nullable ValueAnimator currentAnimator = null;


    //region Lifecycle

    public PanImageView(Context context) {
        this(context, null, 0);
    }

    public PanImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.PanImageView, defStyleAttr, 0);
            setImageDrawable(attributes.getDrawable(R.styleable.PanImageView_panImage));
            setPanAmount(attributes.getFloat(R.styleable.PanImageView_panAmount, 0f));
            attributes.recycle();
        }

        setBackgroundColor(Color.BLACK);
    }

    //endregion


    //region Drawing

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        calculateScaledImageWidth();
    }

    protected void calculateScaledImageWidth() {
        if (image != null) {
            float viewHeight = getMeasuredHeight();
            float imageHeight = image.getIntrinsicHeight();
            float imageWidth = image.getIntrinsicWidth();
            if (viewHeight > imageHeight) {
                float scaleY = viewHeight / imageHeight;
                this.scaledImageWidth = Math.round(imageWidth * scaleY);
            } else if (imageHeight > 0f) {
                float scaleY = imageHeight / viewHeight;
                this.scaledImageWidth = Math.round(imageWidth * scaleY);
            } else {
                this.scaledImageWidth = 0;
            }
        } else {
            this.scaledImageWidth = 0;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (image != null) {
            int offScreenWidth = scaledImageWidth - canvas.getWidth();
            int panOffset = Math.round(offScreenWidth * panAmount);
            image.setBounds(-panOffset, 0, scaledImageWidth - panOffset, canvas.getHeight());
            image.draw(canvas);
        }
    }

    //endregion


    //region Attributes

    public void setImageDrawable(@Nullable Drawable image) {
        this.image = image;
        this.panAmount = 0;
        this.scaledImageWidth = 0;

        calculateScaledImageWidth();
        invalidate();
    }

    public void setImageResource(@DrawableRes int drawableRes) {
        setImageDrawable(getResources().getDrawable(drawableRes));
    }

    public void setPanAmount(float panAmount) {
        clearAnimation();

        if (panAmount > 1f) {
            panAmount = 1f;
        } else if (panAmount < 0f) {
            panAmount = 0f;
        }

        this.panAmount = panAmount;

        invalidate();
    }

    //endregion


    //region Animation

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
    }

    public void animateToPanAmount(float endAmount, long duration, @Nullable Runnable onCompletion) {
        if (currentAnimator == null && panAmount == endAmount) {
            if (onCompletion != null) {
                onCompletion.run();
            }

            return;
        }

        clearAnimation();

        this.currentAnimator = ValueAnimator.ofFloat(panAmount, endAmount);
        currentAnimator.setInterpolator(Animation.INTERPOLATOR_DEFAULT);
        currentAnimator.setDuration(duration);
        currentAnimator.addUpdateListener(a -> {
            this.panAmount = (float) a.getAnimatedValue();
            invalidate();
        });
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (currentAnimator == animation) {
                    PanImageView.this.currentAnimator = null;
                }

                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        });

        currentAnimator.start();
    }

    //endregion
}
