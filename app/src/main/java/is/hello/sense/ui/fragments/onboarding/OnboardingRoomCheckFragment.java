package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.Sensor;
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
            presenterView.setSensorContainerXOffset();
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
        final UnitConverter unitConverter = unitFormatter.getUnitConverterForSensor(currentPositionSensor.getName());
        int convertedValue = 0;
        if (currentPositionSensor.getValue() != null) {
            convertedValue = (int) unitConverter.convert(currentPositionSensor.getValue().longValue());
        }
        presenterView.showConditionAt(position,
                                      currentPositionSensor,
                                      convertedValue,
                                      unitFormatter.getUnitSuffixForSensor(currentPositionSensor.getName()),
                                      () -> presenter.showConditionAt(position + 1));
    }

    @Override
    public void updateSensorView(final List<Sensor> sensors) {
        presenterView.updateSensorView(sensors);
    }

    @Override
    public void jumpToEnd(final boolean animate) {
        this.animationCompleted = true;
        presenterView.stopAnimations();

        presenter.execute(() -> {
            final Condition averageCondition = presenter.calculateAverageCondition();
            presenterView.showCompletion(animate,
                                         averageCondition,
                                         this::continueOnboarding);
        });
    }

    //endregion

    //region Binding

    @Override
    public void createSensorConditionViews(@NonNull final List<String> sensorNames) {
        presenterView.createSensorConditionViews(sensorNames);
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
}
