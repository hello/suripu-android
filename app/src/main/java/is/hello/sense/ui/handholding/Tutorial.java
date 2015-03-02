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
import android.support.annotation.StringRes;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.util.LambdaVar;

public enum Tutorial {
    SLEEP_SCORE_BREAKDOWN(R.string.app_name, Gravity.BOTTOM, R.id.fragment_timeline_sleep_score_chart, Interaction.TAP);

    public final @StringRes int descriptionRes;
    public final int descriptionGravity;
    public final @IdRes int anchorId;
    public final Interaction interaction;

    private Tutorial(@StringRes int descriptionRes,
                     int descriptionGravity,
                     @IdRes int anchorId,
                     @NonNull Interaction interaction) {
        this.descriptionRes = descriptionRes;
        this.descriptionGravity = descriptionGravity;
        this.anchorId = anchorId;
        this.interaction = interaction;
    }


    //region Vending Animations

    public static Animator createPulseAnimation(@NonNull View view) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0.8f, 1f);
        animator.setStartDelay(350);
        animator.setDuration(1250);
        animator.setInterpolator(new AnticipateOvershootInterpolator());
        animator.addUpdateListener(a -> {
            float scale = (float) a.getAnimatedValue();
            view.setScaleX(scale);
            view.setScaleY(scale);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            boolean canceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled) {
                    animation.start();
                }
            }
        });
        return animator;
    }

    public static Animator createSlideAnimation(@NonNull View view,
                                                @DimenRes int deltaRes,
                                                boolean isVertical) {
        Property<View, Float> property;
        if (isVertical) {
            property = Property.of(View.class, float.class, "translationY");
        } else {
            property = Property.of(View.class, float.class, "translationX");
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
        switch (interaction) {
            case TAP:
                return createPulseAnimation(view);

            case SLIDE_LEFT:
                return createSlideAnimation(view, R.dimen.interaction_slide_negative, interaction.isVertical);

            case SLIDE_RIGHT:
                return createSlideAnimation(view, R.dimen.interaction_slide_positive, interaction.isVertical);

            case SLIDE_UP:
                return createSlideAnimation(view, R.dimen.interaction_slide_negative, interaction.isVertical);

            case SLIDE_DOWN:
                return createSlideAnimation(view, R.dimen.interaction_slide_positive, interaction.isVertical);

            default:
                throw new IllegalStateException();
        }
    }

    //endregion


    public RelativeLayout.LayoutParams generateDescriptionLayoutParams() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        switch (descriptionGravity) {
            case Gravity.TOP: {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                break;
            }
            case Gravity.BOTTOM: {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown gravity constant " + descriptionGravity);
            }
        }
        return layoutParams;
    }
}
