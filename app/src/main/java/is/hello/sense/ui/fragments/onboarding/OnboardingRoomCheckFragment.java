package is.hello.sense.ui.fragments.onboarding;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.mvp.view.onboarding.RoomCheckView;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.RoomCheckPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingRoomCheckFragment extends BasePresenterFragment
implements RoomCheckPresenter.Output{

    @Inject
    RoomCheckPresenter presenter;
    @Inject UnitFormatter unitFormatter;

    private boolean animationCompleted = false;

    private RoomCheckView presenterView;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.animationCompleted = (savedInstanceState != null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        this.presenterView = new RoomCheckView(getActivity(), getAnimatorContext());
        return presenterView;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (animationCompleted) {
            presenter.jumpToEnd(false);
        } else {
            presenterView.initSensorContainerXOffset();
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

        presenterView.stopAnimations();
        presenterView.releaseViews();
        presenterView = null;
    }


    //region Scene Animation
    @Override
    public void showConditionAt(final int position,
                                final Sensor currentPositionSensor) {
        presenterView.animateSenseToGray();
        final UnitConverter unitConverter = unitFormatter.getUnitConverterForSensor(currentPositionSensor.getType());
        int convertedValue = 0;
        if (currentPositionSensor.getValue() != null) {
            convertedValue = (int) unitConverter.convert(currentPositionSensor.getValue().longValue());
        }
        final Condition condition = currentPositionSensor.getCondition();
        presenterView.showConditionAt(position,
                                      currentPositionSensor.getType(),
                                      currentPositionSensor.getMessage(),
                                      getConditionDrawable(condition),
                                      condition.colorRes,
                                      convertedValue,
                                      unitFormatter.getSuffixForSensor(currentPositionSensor.getType()),
                                      () -> presenter.showConditionAt(position + 1));
    }

    @Override
    public void updateSensorView(final List<Sensor> sensors) {
        for(int position = 0; position < sensors.size(); position++) {
            final Sensor sensor = sensors.get(position);
            presenterView.updateSensorView(position, sensor.getColor(getActivity()), sensor.getType());
        }
    }

    @Override
    public void jumpToEnd(final boolean animate) {
        this.animationCompleted = true;
        presenterView.stopAnimations();

        presenter.execute(() -> {
            final Condition averageCondition = presenter.calculateAverageCondition();
            presenterView.animateSenseCondition(animate, getConditionDrawable(averageCondition));
            presenterView.showCompletion(animate,
                                         this::continueOnboarding);
        });
    }

    //endregion

    //region Binding

    @Override
    public void createSensorConditionViews(@NonNull final List<SensorType> types) {
        presenterView.createSensorConditionViews(types);
        presenter.showConditionAt(0);
    }

    @Override
    public void unavailableConditions(final Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);

        presenterView.removeSensorContainerViews();
        jumpToEnd(true);
    }

    //endregion


    public void continueOnboarding(@NonNull final View sender) {
        finishFlow();
    }

    private Drawable getConditionDrawable(@NonNull final Condition condition) {
        final Resources resources = getResources();
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
                return presenterView.getDefaultSenseCondition();
            }
        }
    }
}
