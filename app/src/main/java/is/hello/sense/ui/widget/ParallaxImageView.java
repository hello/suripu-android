package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;

public class ParallaxImageView extends View implements Target {
    private final Drawable.Callback DRAWABLE_CALLBACK = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            ParallaxImageView.this.invalidate();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            ParallaxImageView.this.scheduleDrawable(who, what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            ParallaxImageView.this.unscheduleDrawable(who, what);
        }
    };

    private int clip = 0;
    private float parallaxPercent;
    private float aspectRatioScale;

    private int drawableWidth, drawableHeight;
    private @Nullable Drawable drawable;
    private @Nullable AnimatorContext animatorContext;
    private @Nullable ValueAnimator drawableFadeIn;


    //region Lifecycle

    public ParallaxImageView(@NonNull Context context) {
        this(context, null);
    }

    public ParallaxImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParallaxImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();

        final int defaultClip = resources.getDimensionPixelSize(R.dimen.view_parallax_image_clip);
        setClip(defaultClip);
        setAspectRatio(1f, 2f);
    }

    //endregion


    //region Rendering

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int totalClipHeight = clip * 2;
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int scaledHeight = Math.round(width * aspectRatioScale) - totalClipHeight;
        final int height = getDefaultSize(scaledHeight, heightMeasureSpec);

        this.drawableWidth = width;
        this.drawableHeight = height + totalClipHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawable != null) {
            final int top = Math.round(-clip + (clip * parallaxPercent));
            final int bottom = top + drawableHeight;
            drawable.setBounds(0, top, drawableWidth, bottom);
            drawable.draw(canvas);
        }
    }

    //endregion


    //region Animations

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (drawableFadeIn != null) {
            drawableFadeIn.cancel();
        }
    }

    private void fadeIn() {
        if (drawable == null) {
            invalidate();
            return;
        }

        this.drawableFadeIn = AnimatorTemplate.DEFAULT.apply(ValueAnimator.ofInt(0, 255));
        drawableFadeIn.addUpdateListener(animator -> {
            final int alpha = (int) animator.getAnimatedValue();
            drawable.setAlpha(alpha);
        });
        drawableFadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation == drawableFadeIn) {
                    ParallaxImageView.this.drawableFadeIn = null;
                }
            }
        });
        if (animatorContext != null) {
            animatorContext.bind(drawableFadeIn, "ParallaxImageView#drawableFadeIn");
        }
        drawableFadeIn.start();
    }

    //endregion

    //region Attributes

    public void setDrawable(@Nullable Drawable drawable) {
        clearAnimation();

        if (this.drawable != null) {
            this.drawable.setCallback(null);
        }

        final boolean wantsFadeIn = (this.drawable == null && drawable != null);

        this.drawable = drawable;

        if (drawable != null) {
            drawable.setCallback(DRAWABLE_CALLBACK);
        }

        if (wantsFadeIn) {
            fadeIn();
        } else {
            invalidate();
        }
    }

    public void setClip(int clip) {
        this.clip = clip;
        invalidate();
    }

    public void setParallaxPercent(float parallaxPercent) {
        this.parallaxPercent = parallaxPercent;
        invalidate();
    }

    public void setAspectRatio(float x, float y) {
        this.aspectRatioScale = x / y;
        invalidate();
    }

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    //endregion


    //region Picasso Support

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        setDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        setDrawable(errorDrawable);
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        setDrawable(placeHolderDrawable);
    }

    //endregion
}
