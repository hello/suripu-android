package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ParallaxImageView extends FrameLayout implements Target {
    public static final float ASPECT_RATIO_SCALE_DEFAULT = 0.5f /*2:1*/;

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
    private @Nullable PicassoListener picassoListener;
    private @IdRes int loadingViewId = View.NO_ID;
    private @Nullable View loadingView;

    private @Nullable ValueAnimator drawableFadeIn;
    private @Nullable Animator parallaxPercentAnimator;


    //region Lifecycle

    public static float parseAspectRatio(@NonNull String string) throws NumberFormatException {
        final String[] components = string.split(":");
        if (components.length == 2) {
            return Float.parseFloat(components[1]) / Float.parseFloat(components[0]);
        } else if (components.length == 1) {
            return Float.parseFloat(components[0]);
        } else {
            throw new NumberFormatException("Malformed aspect ratio '" + string + "'");
        }
    }

    public ParallaxImageView(@NonNull Context context) {
        this(context, null);
    }

    public ParallaxImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParallaxImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(false);

        final Resources resources = getResources();
        final int defaultClip = resources.getDimensionPixelSize(R.dimen.view_parallax_image_clip);
        if (attrs != null) {
            final TypedArray values = context.obtainStyledAttributes(attrs,
                                                                     R.styleable.ParallaxImageView,
                                                                     defStyle, 0);

            this.clip =
                    values.getInteger(R.styleable.ParallaxImageView_senseParallaxClip, defaultClip);
            this.parallaxPercent =
                    values.getFloat(R.styleable.ParallaxImageView_senseParallaxPercent, 0f);

            final String aspectRatio =
                    values.getString(R.styleable.ParallaxImageView_senseAspectRatio);
            if (!TextUtils.isEmpty(aspectRatio)) {
                this.aspectRatioScale = parseAspectRatio(aspectRatio);
            } else {
                this.aspectRatioScale = ASPECT_RATIO_SCALE_DEFAULT;
            }

            if (values.hasValue(R.styleable.ParallaxImageView_senseLoadingView)) {
                this.loadingViewId =
                        values.getResourceId(R.styleable.ParallaxImageView_senseLoadingView,
                                             View.NO_ID);
            }

            values.recycle();
        } else {
            this.clip = defaultClip;
            this.aspectRatioScale = ASPECT_RATIO_SCALE_DEFAULT;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (loadingViewId != View.NO_ID) {
            this.loadingView = findViewById(loadingViewId);
        }
    }

    //endregion


    //region Rendering

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int totalClipHeight = clip * 2;
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = Math.round(width * aspectRatioScale) - totalClipHeight;

        this.drawableWidth = width;
        this.drawableHeight = height + totalClipHeight;

        // Have to call super, otherwise the FrameLayout will fail.
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
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

        if (parallaxPercentAnimator != null) {
            parallaxPercentAnimator.cancel();
        }

        if (loadingView != null) {
            Anime.cancelAll(loadingView);
        }
    }

    private void fadeIn() {
        if (drawable == null) {
            if (loadingView != null) {
                loadingView.setVisibility(VISIBLE);
            }
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
            public void onAnimationCancel(Animator animation) {
                drawable.setAlpha(255);
            }

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

        if (loadingView != null) {
            animatorFor(loadingView, animatorContext)
                    .fadeOut(INVISIBLE)
                    .start();
        }
    }

    //endregion

    //region Attributes

    public void setDrawable(@Nullable Drawable drawable, boolean animate) {
        clearAnimation();

        if (drawable == this.drawable) {
            return;
        }

        if (this.drawable != null) {
            this.drawable.setCallback(null);
        }

        final boolean wantsFadeIn = (animate && this.drawable == null && drawable != null);

        this.drawable = drawable;

        if (drawable != null) {
            drawable.setCallback(DRAWABLE_CALLBACK);
        }

        if (wantsFadeIn) {
            fadeIn();
        } else {
            if (loadingView != null) {
                if (drawable != null) {
                    loadingView.setVisibility(INVISIBLE);
                } else {
                    loadingView.setVisibility(VISIBLE);
                }
            }

            invalidate();
        }
    }

    @Nullable
    public Drawable getDrawable() {
        return drawable;
    }

    public void setClip(int clip) {
        this.clip = clip;
        invalidate();
    }

    public void setParallaxPercent(float parallaxPercent) {
        this.parallaxPercent = parallaxPercent;
        if (drawable != null) {
            invalidate();
        }
    }

    public float getParallaxPercent() {
        return parallaxPercent;
    }

    public void setAspectRatioScale(float aspectRatioScale) {
        this.aspectRatioScale = aspectRatioScale;
        invalidate();
    }

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setPicassoListener(@Nullable PicassoListener picassoListener) {
        this.picassoListener = picassoListener;
    }

    public void showLoadingView() {
        if (loadingView != null) {
            loadingView.setAlpha(1f);
            loadingView.setVisibility(VISIBLE);
        }
    }

    @NonNull
    public Animator createParallaxPercentAnimator(float targetPercent) {
        if (parallaxPercentAnimator != null) {
            parallaxPercentAnimator.cancel();
        }

        this.parallaxPercentAnimator = ObjectAnimator.ofFloat(this, "parallaxPercent",
                                                              parallaxPercent, targetPercent);

        //noinspection ConstantConditions -- ObjectAnimator is missing nullability annotations
        parallaxPercentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (ParallaxImageView.this.parallaxPercentAnimator == animation) {
                    ParallaxImageView.this.parallaxPercentAnimator = null;
                }
            }
        });

        if (animatorContext != null) {
            animatorContext.bind(parallaxPercentAnimator,
                                 "ParallaxImageView#parallaxPercentAnimator");
        }

        return parallaxPercentAnimator;
    }

    //endregion


    //region Picasso Support

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        setDrawable(new BitmapDrawable(getResources(), bitmap), true);

        if (picassoListener != null) {
            picassoListener.onBitmapLoaded(bitmap);
        }
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        setDrawable(errorDrawable, true);

        if (picassoListener != null) {
            picassoListener.onBitmapFailed();
        }
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        setDrawable(placeHolderDrawable, true);
    }

    //endregion


    public interface PicassoListener {
        void onBitmapLoaded(@NonNull Bitmap bitmap);
        void onBitmapFailed();
    }
}
