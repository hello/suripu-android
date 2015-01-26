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
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;

public class PanImageView extends View {
    private @Nullable Drawable image;
    private float panAmount = 0f;
    private int scaledImageWidth = 0;

    private @Nullable ValueAnimator currentAnimator = null;

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


    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        calculateScaledImageWidth();
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (currentAnimator != null) {
            currentAnimator.cancel();
            this.currentAnimator = null;
        }
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
        if (panAmount > 1f) {
            panAmount = 1f;
        } else if (panAmount < 0f) {
            panAmount = 0f;
        }

        this.panAmount = panAmount;

        invalidate();
    }

    public void animateToPanAmount(float endAmount, long duration, @Nullable Runnable onCompletion) {
        if (panAmount == endAmount) {
            if (onCompletion != null) {
                onCompletion.run();
            }

            return;
        }

        this.currentAnimator = ValueAnimator.ofFloat(panAmount, endAmount);
        currentAnimator.setInterpolator(Animations.INTERPOLATOR_DEFAULT);
        currentAnimator.setDuration(duration);
        currentAnimator.addUpdateListener(a -> {
            this.panAmount = (float) a.getAnimatedValue();
            invalidate();
        });
        if (onCompletion != null) {
            currentAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    PanImageView.this.currentAnimator = null;
                    onCompletion.run();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    PanImageView.this.currentAnimator = null;
                }
            });
        }
        currentAnimator.start();
    }
}
