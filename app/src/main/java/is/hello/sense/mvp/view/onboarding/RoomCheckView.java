package is.hello.sense.mvp.view.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.Anime.cancelAll;
import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class RoomCheckView extends PresenterView {
    private final ImageView sense;
    private final LinearLayout sensorViewContainer;
    private final LinearLayout dynamicContent;
    private final TextView status;
    private final TextView scoreTicker;
    private final TextView scoreUnit;
    private final Drawable graySense;

    private final int startColor;

    private @Nullable
    SensorConditionView animatingSensorView;
    private @Nullable
    ValueAnimator scoreAnimator;

    private final AnimatorContext animatorContext;
    private TimeInterpolator sensorContainerInterpolator;
    private final Resources resources;

    public RoomCheckView(@NonNull final Activity activity,
                         @NonNull final AnimatorContext animatorContext){
        super(activity);

        this.sense = (ImageView) findViewById(R.id.fragment_onboarding_room_check_sense);
        this.sensorViewContainer = (LinearLayout) findViewById(R.id.fragment_onboarding_room_check_sensors);
        this.dynamicContent = (LinearLayout) findViewById(R.id.fragment_onboarding_room_check_content);
        this.status = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_status);
        this.scoreTicker = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_score_tv);
        this.scoreUnit = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_score_unit_tv);
        this.animatorContext = animatorContext;
        this.sensorContainerInterpolator = new OvershootInterpolator(1.0f);

        this.resources = getResources();
        this.graySense = ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_grey, null);
        this.startColor = ContextCompat.getColor(context, Condition.ALERT.colorRes);

        this.sensorViewContainer.setClickable(false);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_onboarding_room_check;
    }

    @Override
    public void releaseViews() {

        sensorContainerInterpolator = null;
        animatingSensorView = null;
        scoreAnimator = null;
    }

    public void setSensorContainerInterpolator(@NonNull final TimeInterpolator interpolator){
        this.sensorContainerInterpolator = interpolator;
    }

    //todo specifically for onboarding
    public void initSensorContainerXOffset() {
        sensorViewContainer.post( () -> {
            sensorViewContainer.setX(sense.getX() + sense.getWidth() / 2
                                             - resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_view_width) );
            sensorViewContainer.invalidate();
        });
    }

    private void animateSensorContainerTranslateX(final int pixelOffset){
        animatorFor(sensorViewContainer, animatorContext)
                .withInterpolator(sensorContainerInterpolator)
                .translationX(sensorViewContainer.getTranslationX() - pixelOffset)
                .start();
    }

    public void createSensorConditionView(@DrawableRes final int icon,
                                          final int padding,
                                          final LinearLayout.LayoutParams layoutParams) {
        final SensorConditionView conditionView = new SensorConditionView(context);
        final Drawable iconDrawable = ResourcesCompat.getDrawable(resources, icon, null);
        conditionView.setIcon(iconDrawable);
        conditionView.setAnimatorContext(animatorContext);
        conditionView.setPadding(padding,0,padding,0);
        sensorViewContainer.addView(conditionView, layoutParams);
    }

    public void showConditionAt(final int position,
                                @StringRes final int checkStatusMessage,
                                @NonNull final String statusMessage,
                                @DrawableRes final int finalIcon,
                                @Nullable final Drawable senseCondition,
                                @ColorRes final int conditionEndColorRes,
                                final int convertedUnitTickerValue,
                                @NonNull final String unitSuffix,
                                @NonNull final Runnable onComplete) {
        final View childAt = sensorViewContainer.getChildAt(position);
        if(!(childAt instanceof SensorConditionView)){
            onComplete.run();
            return;
        }
        final SensorConditionView sensorView = (SensorConditionView) childAt;
        this.animatingSensorView = sensorView;
        if(position != 0) {
            animateSensorContainerTranslateX(sensorView.getWidth());
        }

        status.setText(checkStatusMessage);
        sensorView.fadeInProgressIndicator(() -> {
            final int endColor = ContextCompat.getColor(context, conditionEndColorRes);
            scoreUnit.setText(unitSuffix);
            scoreAnimator = ValueAnimator.ofInt(0, convertedUnitTickerValue);
            scoreAnimator.setDuration(Anime.DURATION_SLOW);
            scoreAnimator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
            scoreAnimator.addUpdateListener(a -> {
                final int color = Anime.interpolateColors(a.getAnimatedFraction(), startColor, endColor);
                scoreTicker.setText(Integer.toString((int) a.getAnimatedValue(), 10));
                sensorView.setTint(color);
                scoreTicker.setTextColor(color);
                scoreUnit.setTextColor(color);
            });

            scoreAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(final Animator animation) {
                    scoreTicker.setText(convertedUnitTickerValue);
                    scoreTicker.setTextColor(endColor);
                    scoreUnit.setText(unitSuffix);
                    scoreUnit.setTextColor(endColor);
                    sensorView.setTint(endColor);
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    if(RoomCheckView.this.scoreAnimator != null) {
                        RoomCheckView.this.scoreAnimator.removeAllListeners();
                        RoomCheckView.this.scoreAnimator = null;
                    }
                    animatorFor(status, animatorContext)
                            .fadeOut(View.VISIBLE)
                            .addOnAnimationCompleted(finishedStatus -> {
                                if (!finishedStatus) {
                                    return;
                                }

                                status.setText(null);
                                status.setText(statusMessage);

                                animateSenseCondition(senseCondition, false);
                                sensorView.transitionToIcon(finalIcon, onComplete);

                                animatorFor(status, animatorContext)
                                        .fadeIn()
                                        .start();
                            })
                            .start();
                }
            });
            scoreAnimator.start();
        });
    }

    public void setSenseCondition(final boolean animate, @Nullable final Drawable senseCondition){
        if(senseCondition != null) {
            if (animate) {
                animateSenseCondition(senseCondition, true);
            } else {
                sense.setImageDrawable(senseCondition);
            }
        }
    }

    public void showCompletion(final boolean animate,
                               final OnClickListener onContinueClickListener) {

        final LayoutInflater inflater = LayoutInflater.from(context);
        final OnAnimationCompleted atEnd = finishedTransitionIn -> {
            if (!finishedTransitionIn) {
                return;
            }

            final Button continueButton = (Button) dynamicContent.findViewById(R.id.sub_fragment_room_check_end_continue);
            Views.setSafeOnClickListener(continueButton, onContinueClickListener);
        };
        if (animate) {
            animatorFor(dynamicContent, animatorContext)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(onCompleted -> {
                        if(onCompleted){
                            dynamicContent.removeAllViews();
                            inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);

                            animatorFor(dynamicContent, animatorContext)
                                    .fadeIn()
                                    .addOnAnimationCompleted(atEnd)
                                    .postStart();
                        }
                    }).start();

        } else {
            dynamicContent.removeAllViews();
            inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);
            atEnd.onAnimationCompleted(true);
        }
    }

    public void stopAnimations() {
        cancelAll(status, dynamicContent, sensorViewContainer);

        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }

        if (animatingSensorView != null) {
            animatingSensorView.clearAnimation();
        }
    }

    public void removeSensorContainerViews() {
        sensorViewContainer.removeAllViews();
    }

    //region Animating Sense

    public void animateSenseToGray() {
        final Drawable senseDrawable = sense.getDrawable();
        if (senseDrawable instanceof TransitionDrawable) {
            ((TransitionDrawable) senseDrawable).reverseTransition(Anime.DURATION_NORMAL);
        }
    }

    public Drawable getDefaultSenseCondition() {
        return graySense;
    }

    private void animateSenseCondition(@NonNull final Drawable newSenseCondition, final boolean fromCurrent) {
        final TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] {
                fromCurrent ? sense.getDrawable() : graySense, newSenseCondition,
        });
        transitionDrawable.setCrossFadeEnabled(true);
        sense.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(Anime.DURATION_NORMAL);
    }

    //endregion
}
