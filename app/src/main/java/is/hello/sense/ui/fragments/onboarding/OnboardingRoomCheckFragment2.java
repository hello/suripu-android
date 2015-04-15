package is.hello.sense.ui.fragments.onboarding;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SensorTickerView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Scheduler;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingRoomCheckFragment2 extends InjectionFragment {
    private static final long CONDITION_VISIBLE_MS = 2500;

    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    private ImageView sense;
    private final List<SensorConditionView> sensorViews = new ArrayList<>();
    private TextView status;
    private SensorTickerView ticker;

    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();

    private final List<SensorState> conditions = new ArrayList<>();
    private final @StringRes int[] conditionStrings = {
        R.string.checking_condition_temperature,
        R.string.checking_condition_humidity,
        R.string.checking_condition_sound,
        R.string.checking_condition_light,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter.update();
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_room_check2, container, false);

        this.sense = (ImageView) view.findViewById(R.id.fragment_onboarding_room_check_sense);
        this.status = (TextView) view.findViewById(R.id.fragment_onboarding_room_check_status);
        this.ticker = (SensorTickerView) view.findViewById(R.id.fragment_onboarding_room_check_ticker);

        ViewGroup sensors = (ViewGroup) view.findViewById(R.id.fragment_onboarding_room_check_sensors);
        for (int i = 0, count = sensors.getChildCount(); i < count; i++) {
            View sensorChild = sensors.getChildAt(i);
            if (sensorChild instanceof SensorConditionView) {
                sensorViews.add((SensorConditionView) sensorChild);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.currentConditions,
                         this::bindConditions,
                         this::conditionsUnavailable);
    }


    //region Animations

    private void animateConditionAt(int position) {
        if (position >= conditions.size()) {
            jumpToEnd();
            return;
        }

        SensorConditionView conditionView = sensorViews.get(position);
        status.setTextAppearance(getActivity(), R.style.AppTheme_Text_SectionHeading);
        status.setText(conditionStrings[position]);
        conditionView.crossFadeToFill(R.drawable.room_check_sensor_border_loading, true, () -> {
            SensorState condition = conditions.get(position);

            Resources resources = getResources();
            int startColor = resources.getColor(Condition.ALERT.colorRes);
            int endColor = resources.getColor(condition.getCondition().colorRes);

            ValueAnimator scoreAnimator = Animation.createColorAnimator(startColor, endColor);
            scoreAnimator.setDuration(SensorTickerView.ANIMATION_DURATION_MS);
            scoreAnimator.addUpdateListener(a -> conditionView.setTint((int) a.getAnimatedValue()));

            int value = condition.getValue() != null ? condition.getValue().intValue() : 0;
            ticker.animateToValue(value, endColor, () -> {
                animate(status)
                        .fadeOut(View.VISIBLE)
                        .addOnAnimationCompleted(finished -> {
                            status.setTextAppearance(getActivity(), R.style.AppTheme_Text_Body);
                            status.setText(null);
                            status.setTransformationMethod(null);
                            markdown.renderInto(status, condition.getMessage());

                            animate(status)
                                    .fadeIn()
                                    .start();

                            conditionView.crossFadeToFill(R.drawable.room_check_sensor_border_filled, false, () -> {
                                deferWorker.schedule(() -> animateConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
                            });
                        })
                        .start();
            });
            scoreAnimator.start();
        });
    }

    private void jumpToEnd() {

    }

    //endregion


    //region Binding

    public void bindConditions(@NonNull RoomConditionsPresenter.Result current) {
        conditions.clear();

        conditions.add(current.conditions.getTemperature());
        conditions.add(current.conditions.getHumidity());
        conditions.add(current.conditions.getSound());
        conditions.add(current.conditions.getLight());

        animateConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        conditions.clear();

        jumpToEnd();
    }

    //endregion
}
