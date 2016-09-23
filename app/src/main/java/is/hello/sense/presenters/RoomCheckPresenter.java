package is.hello.sense.presenters;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.functional.Lists;
import is.hello.sense.interactors.SensorResponseInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import rx.Scheduler;

public class RoomCheckPresenter extends BasePresenter<RoomCheckPresenter.Output> {

    @Inject
    SensorResponseInteractor interactor;

    private static final long CONDITION_VISIBLE_MS = 2500;
    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();
    private final List<Sensor> sensors = new ArrayList<>();

    @Override
    public void onDetach() {
        interactor.onContainerDestroyed();
    }

    public void bindAndSubscribeInteractorLatest() {
        bindAndSubscribe(interactor.latest(),
                         this::bindConditions,
                         this::unavailableConditions
                        );
    }

    private void bindConditions(final SensorResponse current) {
        sensors.clear();
        sensors.addAll(current.getSensors());
        final List<SensorType> types = new ArrayList<>(sensors.size());
        for(final Sensor sensor : sensors){
            types.add(sensor.getType());
        }
        view.createSensorConditionViews(types);
    }

    private void unavailableConditions(final Throwable e) {
        sensors.clear();
        view.unavailableConditions(e);
    }

    public void showConditionAt(final int position) {
        if (position >= sensors.size()) {
            jumpToEnd(true);
            return;
        }

        deferWorker.schedule(() -> view.showConditionAt(position, sensors.get(position)),
                             CONDITION_VISIBLE_MS,
                             TimeUnit.MILLISECONDS);
    }

    @Nullable
    public Condition calculateAverageCondition(){
        final int conditionCount = sensors.size();
        if (conditionCount > 0) {
            final int conditionSum = Lists.sumInt(sensors, c -> c.getCondition().ordinal());
            final int conditionAverage = (int) Math.ceil(conditionSum / (float) conditionCount);
            final int conditionOrdinal = Math.min(Condition.IDEAL.ordinal(), conditionAverage);

            view.updateSensorView(sensors);

            return Condition.values()[conditionOrdinal];
        }

        return null;
    }

    public void jumpToEnd(final boolean animate) {
        deferWorker.unsubscribe();
        view.jumpToEnd(animate);
    }

    public interface Output extends BaseOutput {

        void createSensorConditionViews(List<SensorType> types);

        void unavailableConditions(Throwable e);

        void showConditionAt(int position, Sensor currentPositionSensor);

        void updateSensorView(List<Sensor> sensors);

        void jumpToEnd(boolean animate);
    }
}
