package is.hello.sense.ui.fragments.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.RoomCheckPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.Anime.cancelAll;
import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class OnboardingRoomCheckFragment extends BasePresenterFragment
implements RoomCheckPresenter.Output{

    @Inject
    RoomCheckPresenter presenter;

    private ImageView sense;
    private LinearLayout sensorViewContainer;
    private LinearLayout dynamicContent;
    private TextView status;
    private TextView scoreTicker;
    private TextView scoreUnit;
    private Drawable graySense;

    private int startColor;

    private @Nullable
    SensorConditionView animatingSensorView;
    private @Nullable
    ValueAnimator scoreAnimator;

    private TimeInterpolator sensorContainerInterpolator;
    private Button continueButton;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }



    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_room_check, container, false);

        this.sense = (ImageView) view.findViewById(R.id.fragment_onboarding_room_check_sense);
        this.sensorViewContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_sensors);
        this.dynamicContent = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_content);
        this.status = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_status);
        this.scoreTicker = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_score_tv);
        this.scoreUnit = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_score_unit_tv);
        this.sensorContainerInterpolator = new OvershootInterpolator(1.0f);

        this.graySense = ResourcesCompat.getDrawable(getResources(), R.drawable.onboarding_sense_grey, null);
        this.startColor = ContextCompat.getColor(getActivity(), R.color.sensor_empty);

        this.sensorViewContainer.setClickable(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopAnimations();
        releaseViews();
    }

    @Override
    public void onDetach(){
        super.onDetach();
        presenter = null;
    }

    //region RoomCheck Output

    @Override
    public void initialize(){
        initSensorContainerXOffset();
    }

    @Override
    public void createSensorConditionViews(final @DrawableRes int[] icons) {
        removeSensorContainerViews();

        final LinearLayout.LayoutParams layoutParams
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int padding = getResources().getDimensionPixelOffset(R.dimen.item_room_sensor_condition_view_width) / 2;
        for(@DrawableRes final int sensorIconRes : icons) {
            createSensorConditionView(sensorIconRes,
                                      padding,
                                      layoutParams);
        }
    }

    @Override
    public void unavailableConditions(final Throwable e) {
        removeSensorContainerViews();
    }

    @Override
    public void showConditionAt(final int position,
                                @NonNull final String statusMessage,
                                @DrawableRes final int conditionDrawable,
                                @StringRes final int checkStatusMessage,
                                @DrawableRes final int conditionIcon,
                                @ColorRes final int conditionColorRes,
                                final int value,
                                @NonNull final String unitSuffix,
                                @NonNull  final Runnable onComplete) {
        animateSenseToGray();
        showConditionAt(position,
                        checkStatusMessage,
                        statusMessage,
                        conditionIcon,
                        ResourcesCompat.getDrawable(getResources(), conditionDrawable, null),
                        conditionColorRes,
                        value,
                        unitSuffix,
                        onComplete);
    }

    @Override
    public void showCompletion(final boolean animate, @NonNull final Runnable onContinue) {
        stopAnimations();
        setSenseCondition(animate, getDefaultSenseCondition());
        showCompletion(animate,
                       ignored -> onContinue.run());
    }

    //endregion

    //region PresenterView

    public void stopAnimations() {
        cancelAll(status, dynamicContent, sensorViewContainer);

        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }

        if (animatingSensorView != null) {
            animatingSensorView.clearAnimation();
        }
    }

    public void releaseViews() {
        sensorContainerInterpolator = null;
        animatingSensorView = null;
        scoreAnimator = null;

        if(continueButton != null){
            continueButton.setOnClickListener(null);
        }
    }

    public void setSensorContainerInterpolator(@NonNull final TimeInterpolator interpolator){
        this.sensorContainerInterpolator = interpolator;
    }

    //todo specifically for onboarding
    public void initSensorContainerXOffset() {
        sensorViewContainer.post( () -> {
            sensorViewContainer.setX(sense.getX() + sense.getWidth() / 2
                                             - getResources().getDimensionPixelSize(R.dimen.item_room_sensor_condition_view_width) );
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
        final SensorConditionView conditionView = new SensorConditionView(getActivity());
        final Drawable iconDrawable = ResourcesCompat.getDrawable(getResources(), icon, null);
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
            final int endColor = ContextCompat.getColor(getActivity(), conditionEndColorRes);
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
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    if(OnboardingRoomCheckFragment.this.scoreAnimator != null) {
                        OnboardingRoomCheckFragment.this.scoreAnimator.removeAllListeners();
                        OnboardingRoomCheckFragment.this.scoreAnimator = null;
                    }
                    if(status == null || OnboardingRoomCheckFragment.this.isRemoving()){
                        return;
                    }
                    animatorFor(status, animatorContext)
                            .fadeOut(View.VISIBLE)
                            .addOnAnimationCompleted(finishedStatus -> {
                                if (!finishedStatus || OnboardingRoomCheckFragment.this.isRemoving()) {
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
                               final View.OnClickListener onContinueClickListener) {

        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final OnAnimationCompleted atEnd = finishedTransitionIn -> {
            if (!finishedTransitionIn) {
                return;
            }

            continueButton = (Button) dynamicContent.findViewById(R.id.sub_fragment_room_check_end_continue);
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
