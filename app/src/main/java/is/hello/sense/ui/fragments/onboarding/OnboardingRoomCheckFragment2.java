package is.hello.sense.ui.fragments.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Scheduler;

public class OnboardingRoomCheckFragment2 extends InjectionFragment {
    private static final long CONDITION_VISIBLE_MS = 2500;
    private static final long COUNT_UP_DURATION_MS = 850;

    @Inject RoomConditionsPresenter presenter;

    private ImageView sense;
    private final List<SensorConditionView> sensorViews = new ArrayList<>();
    private TextView status;
    private TextView ticker;

    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();

    private final List<SensorState> conditions = new ArrayList<>();
    private final List<UnitSystem.Formatter> conditionFormatters = new ArrayList<>();

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
        this.ticker = (TextView) view.findViewById(R.id.fragment_onboarding_room_check_ticker);

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
        conditionView.setProgressState();

        SensorState condition = conditions.get(position);
        UnitSystem.Formatter formatter = conditionFormatters.get(position);

        int value = condition.getValue() != null ? condition.getValue().intValue() : 0;
        ValueAnimator scoreAnimator = ValueAnimator.ofInt(0, value);
        AnimatorConfig.DEFAULT.apply(scoreAnimator);
        scoreAnimator.setDuration(COUNT_UP_DURATION_MS);

        Resources resources = getResources();
        int startColor = resources.getColor(Condition.ALERT.colorRes);
        int endColor = resources.getColor(condition.getCondition().colorRes);
        ArgbEvaluator colorEvaluator = new ArgbEvaluator();

        scoreAnimator.addUpdateListener(a -> {
            int color = (int) colorEvaluator.evaluate(a.getAnimatedFraction(), startColor, endColor);
            ticker.setTextColor(color);
            ticker.setText(formatter.format(((Integer) a.getAnimatedValue()).longValue()));
        });

        scoreAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                conditionView.setCompletedState(condition.getCondition());
                deferWorker.schedule(() -> animateConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
            }
        });
        scoreAnimator.start();
    }

    private void jumpToEnd() {

    }

    //endregion


    //region Binding

    public void bindConditions(@NonNull RoomConditionsPresenter.Result current) {
        conditions.clear();
        conditionFormatters.clear();

        conditions.add(current.conditions.getTemperature());
        conditionFormatters.add(current.units::formatTemperature);

        conditions.add(current.conditions.getHumidity());
        conditionFormatters.add(current.units::formatHumidity);

        conditions.add(current.conditions.getSound());
        conditionFormatters.add(current.units::formatDecibels);

        conditions.add(current.conditions.getLight());
        conditionFormatters.add(current.units::formatLight);

        animateConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        conditions.clear();
        conditionFormatters.clear();

        jumpToEnd();
    }

    //endregion
}
