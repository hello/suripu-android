package is.hello.sense.ui.fragments.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.presenters.RoomCheckPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SensorTickerView;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

import static is.hello.go99.Anime.cancelAll;
import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class OnboardingRoomCheckFragment extends BasePresenterFragment
implements RoomCheckPresenter.Output{

    @Inject
    RoomCheckPresenter presenter;
    @Inject UnitFormatter unitFormatter;

    private ImageView sense;
    private LinearLayout sensorViewContainer;
    private LinearLayout dynamicContent;
    private TextView status;
    private SensorTickerView scoreTicker;
    //todo refactor
    private int startColor;
    private boolean animationCompleted = false;

    private Resources resources;
    private Drawable graySense;
    private @Nullable SensorConditionView animatingSensorView;
    private @Nullable ValueAnimator scoreAnimator;

    @Override
    public void onInjected() {
        addScopedPresenter(presenter);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.animationCompleted = (savedInstanceState != null);

        this.resources = getResources();
        this.graySense = ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_grey, null);
        this.startColor = ContextCompat.getColor(getActivity(),Condition.ALERT.colorRes);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_room_check, container, false);

        this.sense = (ImageView) view.findViewById(R.id.fragment_onboarding_room_check_sense);
        this.sensorViewContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_sensors);

        this.dynamicContent = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_content);
        this.status = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_status);

        final AnimatorContext animatorContext = getAnimatorContext();
        this.scoreTicker = (SensorTickerView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_ticker);
        scoreTicker.setAnimatorContext(animatorContext);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (animationCompleted) {
            presenter.jumpToEnd(false);
        } else {
            presenter.bindAndSubscribeInteractorLatest();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        presenter.jumpToEnd(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopAnimations();
    }


    //region Scene Animation
    @Override
    public void showConditionAt(final int position,
                                final SensorState currentPositionSensor) {
        animateSenseToGray();


        final SensorConditionView sensorView = (SensorConditionView) sensorViewContainer.getChildAt(position);
        final String sensorName = currentPositionSensor.getName();

        status.setTextAppearance(status.getContext(), R.style.AppTheme_Text_SectionHeading_Large);
        status.setText(getStatusStringForSensor(sensorName));
        Drawing.setLetterSpacing(status, Styles.LETTER_SPACING_SECTION_HEADING_LARGE);
        this.animatingSensorView = sensorView;
        sensorView.fadeInProgressIndicator(() -> {
            int convertedValue = 0;
            if (currentPositionSensor.getValue() != null) {
                UnitConverter converter = unitFormatter.getUnitConverterForSensor(sensorName);
                convertedValue = (int) converter.convert(currentPositionSensor.getValue().longValue());
            }
            final String unitSuffix = unitFormatter.getUnitSuffixForSensor(sensorName);
            final long duration = scoreTicker.animateToValue(convertedValue, unitSuffix, finishedTicker -> {
                if (!finishedTicker) {
                    return;
                }

                animatorFor(status, getAnimatorContext())
                        .fadeOut(View.VISIBLE)
                        .addOnAnimationCompleted(finishedStatus -> {
                            if (!finishedStatus) {
                                return;
                            }

                            status.setTextAppearance(status.getContext(), R.style.AppTheme_Text_Body);
                            status.setText(null);
                            status.setTransformationMethod(null);
                            Drawing.setLetterSpacing(status, 0f);
                            status.setText(currentPositionSensor.getMessage());

                            animateSenseCondition(currentPositionSensor.getCondition(), false);
                            sensorView.transitionToIcon(getFinalIconForSensor(sensorName), () -> {
                                presenter.showConditionAt(position + 1);
                            });

                            animatorFor(status, getAnimatorContext())
                                    .fadeIn()
                                    .start();
                        })
                        .start();
            });

            final int endColor = ContextCompat.getColor(getActivity(), currentPositionSensor.getCondition().colorRes);
            this.scoreAnimator = AnimatorTemplate.DEFAULT.createColorAnimator(startColor, endColor);
            scoreAnimator.setDuration(duration);
            scoreAnimator.addUpdateListener(a -> {
                final int color = (int) a.getAnimatedValue();
                sensorView.setTint(color);
                scoreTicker.setTextColor(color);
            });
            scoreAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    sensorView.setTint(endColor);
                    scoreTicker.setTextColor(endColor);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    OnboardingRoomCheckFragment.this.scoreAnimator = null;
                }
            });
            scoreAnimator.start();
        });
    }

    @Override
    public void updateSensorView(final List<SensorState> sensors) {
        for (int i = 0; i < sensors.size(); i++) {
            final SensorConditionView sensorView = (SensorConditionView) sensorViewContainer.getChildAt(i);
            final SensorState sensor = sensors.get(i);
            sensorView.clearAnimation();
            sensorView.setTint(ContextCompat.getColor(getActivity(), sensor.getCondition().colorRes));
            sensorView.setIcon(getFinalIconForSensor(sensor.getName()));
        }
    }

    @Override
    public void jumpToEnd(final boolean animate) {
        this.animationCompleted = true;
        stopAnimations();

        presenter.execute(() -> {
            final Condition averageCondition = presenter.calculateAverageCondition();
            if(averageCondition != null) {
                if (animate) {
                    animateSenseCondition(averageCondition, true);
                } else {
                    sense.setImageDrawable(getConditionDrawable(averageCondition));
                }
            }

            showCompletion(animate);
        });
    }

    private void showCompletion(final boolean animate) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final OnAnimationCompleted atEnd = finishedTransitionIn -> {
            if (!finishedTransitionIn) {
                return;
            }

            final Button continueButton = (Button) dynamicContent.findViewById(R.id.sub_fragment_room_check_end_continue);
            Views.setSafeOnClickListener(continueButton, this::continueOnboarding);
        };
        if (animate) {
            animatorFor(dynamicContent, getAnimatorContext())
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(finishedFadeOut -> {
                        if (!finishedFadeOut) {
                            return;
                        }

                        dynamicContent.removeAllViews();

                        inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);

                        animatorFor(dynamicContent, getAnimatorContext())
                                .fadeIn()
                                .addOnAnimationCompleted(atEnd)
                                .postStart();
                    })
                    .start();
        } else {
            dynamicContent.removeAllViews();
            inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);
            atEnd.onAnimationCompleted(true);
        }
    }

    private void stopAnimations() {
        scoreTicker.stopAnimating();
        cancelAll(status, dynamicContent);

        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }

        if (animatingSensorView != null) {
            animatingSensorView.clearAnimation();
        }
    }

    //endregion


    //region Animating Sense

    private void animateSenseToGray() {
        final Drawable senseDrawable = sense.getDrawable();
        if (senseDrawable instanceof TransitionDrawable) {
            ((TransitionDrawable) senseDrawable).reverseTransition(Anime.DURATION_NORMAL);
        }
    }

    private Drawable getConditionDrawable(@NonNull final Condition condition) {
        switch (condition) {
            case ALERT: {
                return ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_red, null);
            }

            case WARNING: {
                return ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_yellow, null);
            }

            case IDEAL: {
                return ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_green, null);
            }

            default:
            case UNKNOWN: {
                return graySense;
            }
        }
    }

    private void animateSenseCondition(@NonNull Condition condition, boolean fromCurrent) {
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] {
            fromCurrent ? sense.getDrawable() : graySense,
            getConditionDrawable(condition),
        });
        transitionDrawable.setCrossFadeEnabled(true);
        sense.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(Anime.DURATION_NORMAL);
    }

    //endregion


    //region Binding

    private @StringRes int getStatusStringForSensor(@NonNull final String sensorName) {
        switch (sensorName) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                return R.string.checking_condition_temperature;
            }
            case ApiService.SENSOR_NAME_HUMIDITY: {
                return R.string.checking_condition_humidity;
            }
            case ApiService.SENSOR_NAME_PARTICULATES: {
                return R.string.checking_condition_airquality;
            }
            case ApiService.SENSOR_NAME_LIGHT: {
                return R.string.checking_condition_light;
            }
            case ApiService.SENSOR_NAME_SOUND: {
                return R.string.checking_condition_sound;
            }
            default: {
                return R.string.missing_data_placeholder;
            }
        }
    }

    private @DrawableRes int getInitialIconForSensor(@NonNull final String sensorName) {
        switch (sensorName) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                return R.drawable.room_check_sensor_temperature;
            }
            case ApiService.SENSOR_NAME_HUMIDITY: {
                return R.drawable.room_check_sensor_humidity;
            }
            case ApiService.SENSOR_NAME_PARTICULATES: {
                return R.drawable.room_check_sensor_airquality;
            }
            case ApiService.SENSOR_NAME_LIGHT: {
                return R.drawable.room_check_sensor_light;
            }
            case ApiService.SENSOR_NAME_SOUND: {
                return R.drawable.room_check_sensor_sound;
            }
            default: {
                return 0;
            }
        }
    }

    private @DrawableRes int getFinalIconForSensor(@NonNull final String sensorName) {
        switch (sensorName) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                return R.drawable.room_check_sensor_filled_temperature;
            }
            case ApiService.SENSOR_NAME_HUMIDITY: {
                return R.drawable.room_check_sensor_filled_humidity;
            }
            case ApiService.SENSOR_NAME_PARTICULATES: {
                return R.drawable.room_check_sensor_filled_airquality;
            }
            case ApiService.SENSOR_NAME_LIGHT: {
                return R.drawable.room_check_sensor_filled_light;
            }
            case ApiService.SENSOR_NAME_SOUND: {
                return R.drawable.room_check_sensor_filled_sound;
            }
            default: {
                return 0;
            }
        }
    }

    @Override
    public void bindConditions(@NonNull final List<SensorState> sensors) {
        sensorViewContainer.removeAllViews();

        final Context context = getActivity();
        final Resources resources = getResources();
        final LinearLayout.LayoutParams layoutParams
                = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        for (final SensorState sensor : sensors) {
            final SensorConditionView conditionView = new SensorConditionView(context);
            final int iconForSensor = getInitialIconForSensor(sensor.getName());
            final Drawable iconDrawable = ResourcesCompat.getDrawable(resources, iconForSensor, null);
            conditionView.setIcon(iconDrawable);
            conditionView.setAnimatorContext(animatorContext);
            sensorViewContainer.addView(conditionView, layoutParams);
        }

        presenter.showConditionAt(0);
    }

    @Override
    public void unavailableConditions(final Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        sensorViewContainer.removeAllViews();

        jumpToEnd(true);
    }

    //endregion


    public void continueOnboarding(@NonNull final View sender) {
        getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
    }
}
