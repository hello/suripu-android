package is.hello.sense.ui.fragments.onboarding;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SensorTickerView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Scheduler;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.stop;

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

    private final List<SensorState> conditions = new ArrayList<>();
    private final List<String> conditionUnits = new ArrayList<>();
    private final @StringRes int[] conditionStrings = {
        R.string.checking_condition_temperature,
        R.string.checking_condition_humidity,
        R.string.checking_condition_light,
        R.string.checking_condition_sound,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.animationCompleted = savedInstanceState.getBoolean("animationCompleted", false);
        }

        presenter.update();
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

        this.startColor = getResources().getColor(Condition.ALERT.colorRes);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (animationCompleted) {
            jumpToEnd();
        } else {
            bindAndSubscribe(presenter.currentConditions,
                             this::bindConditions,
                             this::conditionsUnavailable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        jumpToEnd();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopAnimations();
        deferWorker.unsubscribe();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("animationCompleted", animationCompleted);
    }


    //region Animations

    private void showConditionAt(int position) {
        if (position >= conditions.size()) {
            jumpToEnd();
            return;
        }

        animateSenseToGray();

        SensorConditionView conditionView = sensorViews.get(position);
        status.setTextAppearance(getActivity(), R.style.AppTheme_Text_SectionHeading);
        status.setText(conditionStrings[position]);
        conditionView.crossFadeToFill(R.drawable.room_check_sensor_border_loading, true, () -> {
            SensorState sensor = conditions.get(position);

            int value = sensor.getValue() != null ? sensor.getValue().intValue() : 0;
            String unit = conditionUnits.get(position);
            long duration = scoreTicker.animateToValue(value, unit, finishedTicker -> {
                if (!finishedTicker) {
                    return;
                }

                animate(status, getAnimatorContext())
                        .fadeOut(View.VISIBLE)
                        .addOnAnimationCompleted(finishedStatus -> {
                            if (!finishedStatus) {
                                return;
                            }

                            status.setTextAppearance(getActivity(), R.style.AppTheme_Text_Body);
                            status.setText(null);
                            status.setTransformationMethod(null);
                            markdown.renderInto(status, sensor.getMessage());

                            animateSenseCondition(sensor.getCondition());
                            conditionView.crossFadeToFill(R.drawable.room_check_sensor_border_filled, false, () -> {
                                deferWorker.schedule(() -> showConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
                            });
                        })
                        .andThen()
                        .fadeIn()
                        .start();
            });


            int endColor = getResources().getColor(sensor.getCondition().colorRes);
            ValueAnimator scoreAnimator = Animation.createColorAnimator(startColor, endColor);
            scoreAnimator.setDuration(duration);
            scoreAnimator.addUpdateListener(a -> {
                int color = (int) a.getAnimatedValue();
                conditionView.setTint(color);
                scoreTicker.setTextColor(color);
            });
            scoreAnimator.start();
        });
    }

    private void jumpToEnd() {
        int conditionCount = conditions.size();
        if (conditionCount > 0) {
            int conditionSum = Lists.sumInt(conditions, c -> c.getCondition().ordinal());
            int conditionAverage = (int) Math.ceil(conditionSum / (float) conditionCount);
            int conditionOrdinal = Math.min(Condition.IDEAL.ordinal(), conditionAverage);
            Condition condition = Condition.values()[conditionOrdinal];
            animateSenseCondition(condition);
        } else {
            int defaultTint = getResources().getColor(R.color.light_accent);
            for (SensorConditionView sensorView : sensorViews) {
                sensorView.setTint(defaultTint);
                sensorView.setFill(R.drawable.room_check_sensor_border_filled);
            }
        }

        stopAnimations();
        showCompletion();
    }

    private void showCompletion() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        animate(dynamicContent, getAnimatorContext())
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(finishedFadeOut -> {
                    if (!finishedFadeOut) {
                        return;
                    }

                    dynamicContent.removeAllViews();

                    inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);

                    animate(dynamicContent, getAnimatorContext())
                            .fadeIn()
                            .addOnAnimationCompleted(finishedTransitionIn -> {
                                if (!finishedTransitionIn) {
                                    return;
                                }

                                Button continueButton = (Button) dynamicContent.findViewById(R.id.sub_fragment_room_check_end_continue);
                                Views.setSafeOnClickListener(continueButton, this::continueOnboarding);
                            })
                            .postStart();
                })
                .start();
    }

    private void stopAnimations() {
        scoreTicker.stopAnimating();
        stop(status, dynamicContent);
    }

    private void animateSenseToGray() {
        Drawable senseDrawable = sense.getDrawable();
        if (senseDrawable instanceof TransitionDrawable) {
            ((TransitionDrawable) senseDrawable).reverseTransition(Animation.DURATION_NORMAL);
        }
    }

    private void animateSenseCondition(@NonNull Condition condition) {
        int drawableRes = 0;
        switch (condition) {
            case UNKNOWN:
                drawableRes = R.drawable.room_check_sense_gray;
                break;
            case ALERT:
                drawableRes = R.drawable.room_check_sense_red;
                break;
            case WARNING:
                drawableRes = R.drawable.room_check_sense_yellow;
                break;
            case IDEAL:
                drawableRes = R.drawable.room_check_sense_green;
                break;
        }

        Resources resources = getResources();
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] {
            resources.getDrawable(R.drawable.room_check_sense_gray),
            resources.getDrawable(drawableRes),
        });
        transitionDrawable.setCrossFadeEnabled(true);
        sense.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(Animation.DURATION_NORMAL);
    }

    //endregion


    //region Binding

    public void bindConditions(@NonNull RoomConditionsPresenter.Result current) {
        conditions.clear();
        conditions.addAll(current.conditions.toList());

        conditionUnits.clear();
        conditionUnits.addAll(current.units.getUnitNamesAsList());

        showConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        conditions.clear();

        jumpToEnd();
    }

    //endregion


    public void continueOnboarding(@NonNull View sender) {
        ((OnboardingActivity) getActivity()).showSmartAlarmInfo();
    }
}
