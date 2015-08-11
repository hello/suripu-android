package is.hello.sense.ui.fragments.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.OnAnimationCompleted;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SensorTickerView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Scheduler;

import static is.hello.sense.ui.animation.Animation.cancelAll;
import static is.hello.sense.ui.animation.MultiAnimator.animatorFor;
import static is.hello.sense.units.UnitSystem.Unit;

public class OnboardingRoomCheckFragment extends InjectionFragment {
    private static final long CONDITION_VISIBLE_MS = 2500;

    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    private ImageView sense;
    private final List<SensorConditionView> sensorViews = new ArrayList<>();
    private LinearLayout dynamicContent;
    private TextView status;
    private SensorTickerView scoreTicker;

    private int startColor;
    private boolean animationCompleted = false;

    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();

    private final List<SensorState> sensors = new ArrayList<>();
    private final List<Unit> sensorUnits = new ArrayList<>();
    // This order applies to:
    // - RoomSensorHistory
    // - RoomConditions
    // - RoomConditionsFragment
    // - UnitSystem
    // - OnboardingRoomCheckFragment
    private final @StringRes int[] sensorStrings = {
        R.string.checking_condition_temperature,
        R.string.checking_condition_humidity,
        R.string.checking_condition_light,
        R.string.checking_condition_sound,
    };

    private Resources resources;
    private Drawable graySense;
    private @Nullable SensorConditionView animatingSensorView;
    private @Nullable ValueAnimator scoreAnimator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.animationCompleted = (savedInstanceState != null);

        this.resources = getResources();
        this.graySense = ResourcesCompat.getDrawable(resources, R.drawable.room_check_sense_gray, null);
        this.startColor = resources.getColor(Condition.ALERT.colorRes);

        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_room_check, container, false);

        this.sense = (ImageView) view.findViewById(R.id.fragment_onboarding_room_check_sense);
        ViewGroup sensors = (ViewGroup) view.findViewById(R.id.fragment_onboarding_room_check_sensors);
        for (int i = 0, count = sensors.getChildCount(); i < count; i++) {
            View sensorChild = sensors.getChildAt(i);
            if (sensorChild instanceof SensorConditionView) {
                SensorConditionView conditionView = (SensorConditionView) sensorChild;
                conditionView.setAnimatorContext(animatorContext);
                sensorViews.add(conditionView);
            }
        }

        this.dynamicContent = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_content);
        this.status = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_status);

        AnimatorContext animatorContext = getAnimatorContext();
        this.scoreTicker = (SensorTickerView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_ticker);
        scoreTicker.setAnimatorContext(animatorContext);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (animationCompleted) {
            jumpToEnd(false);
        } else {
            bindAndSubscribe(presenter.latest(),
                             this::bindConditions,
                             this::conditionsUnavailable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        jumpToEnd(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopAnimations();
    }


    //region Scene Animation

    private void showConditionAt(int position) {
        if (position >= sensors.size()) {
            jumpToEnd(true);
            return;
        }

        animateSenseToGray();

        SensorConditionView sensorView = sensorViews.get(position);
        status.setTextAppearance(status.getContext(), R.style.AppTheme_Text_SectionHeading);
        status.setText(sensorStrings[position]);
        this.animatingSensorView = sensorView;
        sensorView.crossFadeToFill(R.drawable.room_check_sensor_border_loading, true, () -> {
            SensorState sensor = sensors.get(position);
            Unit unit = sensorUnits.get(position);

            long value = sensor.getValue() != null ? sensor.getValue().longValue() : 0L;
            int convertedValue = (int) unit.convert(value);
            long duration = scoreTicker.animateToValue(convertedValue, unit.getName(), finishedTicker -> {
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
                            markdown.renderInto(status, sensor.getMessage());

                            animateSenseCondition(sensor.getCondition(), false);
                            sensorView.crossFadeToFill(R.drawable.room_check_sensor_border_filled, false, () -> {
                                deferWorker.schedule(() -> showConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
                            });
                        })
                        .andThen()
                        .fadeIn()
                        .start();
            });


            int endColor = resources.getColor(sensor.getCondition().colorRes);
            this.scoreAnimator = AnimatorTemplate.DEFAULT.createColorAnimator(startColor, endColor);
            scoreAnimator.setDuration(duration);
            scoreAnimator.addUpdateListener(a -> {
                int color = (int) a.getAnimatedValue();
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

    private void jumpToEnd(boolean animate) {
        this.animationCompleted = true;
        deferWorker.unsubscribe();
        stopAnimations();

        stateSafeExecutor.execute(() -> {
            int conditionCount = sensors.size();
            if (conditionCount > 0) {
                int conditionSum = Lists.sumInt(sensors, c -> c.getCondition().ordinal());
                int conditionAverage = (int) Math.ceil(conditionSum / (float) conditionCount);
                int conditionOrdinal = Math.min(Condition.IDEAL.ordinal(), conditionAverage);
                Condition averageCondition = Condition.values()[conditionOrdinal];
                if (animate) {
                    animateSenseCondition(averageCondition, true);
                } else {
                    sense.setImageDrawable(getConditionDrawable(averageCondition));
                }

                for (int i = 0; i < conditionCount; i++) {
                    SensorConditionView sensorView = sensorViews.get(i);
                    SensorState condition = sensors.get(i);
                    sensorView.setTint(resources.getColor(condition.getCondition().colorRes));
                    sensorView.setFill(R.drawable.room_check_sensor_border_filled);
                }
            } else {
                int defaultTint = resources.getColor(R.color.light_accent);
                for (SensorConditionView sensorView : sensorViews) {
                    sensorView.setTint(defaultTint);
                    sensorView.setFill(R.drawable.room_check_sensor_border_filled);
                }
            }

            showCompletion(animate);
        });
    }

    private void showCompletion(boolean animate) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        OnAnimationCompleted atEnd = finishedTransitionIn -> {
            if (!finishedTransitionIn) {
                return;
            }

            Button continueButton = (Button) dynamicContent.findViewById(R.id.sub_fragment_room_check_end_continue);
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
            animatingSensorView.stopAnimating();
        }
    }

    //endregion


    //region Animating Sense

    private void animateSenseToGray() {
        Drawable senseDrawable = sense.getDrawable();
        if (senseDrawable instanceof TransitionDrawable) {
            ((TransitionDrawable) senseDrawable).reverseTransition(Animation.DURATION_NORMAL);
        }
    }

    private Drawable getConditionDrawable(@NonNull Condition condition) {
        switch (condition) {
            case ALERT: {
                return ResourcesCompat.getDrawable(resources, R.drawable.room_check_sense_red, null);
            }

            case WARNING: {
                return ResourcesCompat.getDrawable(resources, R.drawable.room_check_sense_yellow, null);
            }

            case IDEAL: {
                return ResourcesCompat.getDrawable(resources, R.drawable.room_check_sense_green, null);
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
        transitionDrawable.startTransition(Animation.DURATION_NORMAL);
    }

    //endregion


    //region Binding

    public void bindConditions(@NonNull RoomConditionsPresenter.Result current) {
        sensors.clear();
        sensors.addAll(current.conditions.toList());

        sensorUnits.clear();
        sensorUnits.addAll(current.units.toUnitList());

        showConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        sensors.clear();

        jumpToEnd(true);
    }

    //endregion


    public void continueOnboarding(@NonNull View sender) {
        ((OnboardingActivity) getActivity()).showSmartAlarmInfo();
    }
}
