package is.hello.sense.ui.handholding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.support.annotation.DimenRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.io.Serializable;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.util.LambdaVar;

public class Interaction implements Serializable {
    public final @IdRes int anchorViewRes;
    public final Type type;

    public Interaction(@NonNull Type type,
                       @IdRes int anchorViewRes) {
        this.type = type;
        this.anchorViewRes = anchorViewRes;
    }


    public Orientation getOrientation() {
        switch (type) {
            case SLIDE_LEFT:
            case SLIDE_RIGHT:
                return Orientation.HORIZONTAL;

            case SLIDE_UP:
            case SLIDE_DOWN:
                return Orientation.VERTICAL;

            default:
            case TAP:
                throw new IllegalStateException("orientation is not valid for tap interactions");
        }
    }


    //region Vending Animations

    public static Animator createPulseAnimation(@NonNull View view) {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.8f);
        animator.setDuration(750);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(a -> {
            float scale = (float) a.getAnimatedValue();
            view.setScaleX(scale);
            view.setScaleY(scale);
        });
        return animator;
    }

    public static Animator createSlideAnimation(@NonNull View view,
                                                @DimenRes int deltaRes,
                                                Orientation orientation) {
        Property<View, Float> property;
        if (orientation == Orientation.HORIZONTAL) {
            property = Property.of(View.class, float.class, "translationX");
        } else {
            property = Property.of(View.class, float.class, "translationY");
        }

        long duration = 750;
        TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();

        float delta = view.getResources().getDimension(deltaRes);
        ObjectAnimator slide = ObjectAnimator.ofFloat(view, property, 0f, delta);
        slide.setInterpolator(interpolator);
        slide.setDuration(duration);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeOut.setInterpolator(interpolator);
        fadeOut.setDuration(duration);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new LinearInterpolator());
        fadeIn.setDuration(Animations.DURATION_MAXIMUM);

        AnimatorSet slideAndFadeIn = new AnimatorSet();
        slideAndFadeIn.setStartDelay(150);
        slideAndFadeIn.play(slide)
                      .with(fadeOut)
                      .before(fadeIn);

        LambdaVar<Boolean> canceled = LambdaVar.of(false);
        slideAndFadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                canceled.set(true);
                fadeIn.cancel();
            }
        });

        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled.get()) {
                    slideAndFadeIn.start();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                property.set(view, 0f);
            }
        });

        return slideAndFadeIn;
    }

    public Animator createAnimation(@NonNull View view) {
        switch (type) {
            case TAP:
                return createPulseAnimation(view);

            case SLIDE_LEFT:
                return createSlideAnimation(view, R.dimen.interaction_slide_negative, Orientation.HORIZONTAL);

            case SLIDE_RIGHT:
                return createSlideAnimation(view, R.dimen.interaction_slide_positive, Orientation.HORIZONTAL);

            case SLIDE_UP:
                return createSlideAnimation(view, R.dimen.interaction_slide_negative, Orientation.VERTICAL);

            case SLIDE_DOWN:
                return createSlideAnimation(view, R.dimen.interaction_slide_positive, Orientation.VERTICAL);

            default:
                throw new IllegalStateException();
        }
    }

    //endregion


    public static enum Orientation {
        VERTICAL,
        HORIZONTAL,
    }

    public static enum Type {
        TAP,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SLIDE_UP,
        SLIDE_DOWN,
    }
}
