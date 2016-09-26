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
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingRoomCheckFragment extends BasePresenterFragment
implements RoomCheckPresenter.Output{

    @Inject
    RoomCheckPresenter presenter;

    private RoomCheckView presenterView;

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
        this.presenterView = new RoomCheckView(getActivity(), getAnimatorContext());
        return presenterView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        presenterView.stopAnimations();
        presenterView.releaseViews();
        presenterView = null;
    }

    //region RoomCheck Output

    @Override
    public void initialize(){
        presenterView.initSensorContainerXOffset();
    }

    @Override
    public void createSensorConditionViews(@NonNull final List<SensorType> types) {
        presenterView.createSensorConditionViews(types);
    }

    @Override
    public void unavailableConditions(final Throwable e) {
        Analytics.trackError(e, "Room check");
        Logger.error(getClass().getSimpleName(), "Could not load conditions for room check", e);
        presenterView.removeSensorContainerViews();
    }

    @Override
    public void showConditionAt(final int position,
                                @NonNull final Sensor currentPositionSensor,
                                final int convertedValue,
                                @NonNull final String unitSuffix,
                                @NonNull  final Runnable onComplete) {
        presenterView.animateSenseToGray();

        final Condition condition = currentPositionSensor.getCondition();
        presenterView.showConditionAt(position,
                                      currentPositionSensor.getType(),
                                      currentPositionSensor.getMessage(),
                                      getConditionDrawable(condition),
                                      condition.colorRes,
                                      convertedValue,
                                      unitSuffix,
                                      onComplete);
    }

    @Override
    public void updateSensorView(final List<Sensor> sensors) {
        for(int position = 0; position < sensors.size(); position++) {
            final Sensor sensor = sensors.get(position);
            presenterView.updateSensorView(position, sensor.getColor(getActivity()), sensor.getType());
        }
    }

    @Override
    public void stopAnimations(){
        presenterView.stopAnimations();
    }

    @Override
    public void animateSenseCondition(final boolean animate, @NonNull final Condition condition){
        presenterView.animateSenseCondition(animate, getConditionDrawable(condition));
    }

    @Override
    public void showCompletion(final boolean animate, @NonNull final Runnable onContinue) {
        presenterView.showCompletion(animate,
                                     ignored -> onContinue.run());
    }

    //endregion

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
