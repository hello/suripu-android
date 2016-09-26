package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.functional.Lists;
import is.hello.sense.interactors.SensorResponseInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.units.UnitFormatter;
import rx.Scheduler;

public class RoomCheckPresenter extends BasePresenter<RoomCheckPresenter.Output> {


    private final SensorResponseInteractor interactor;
    private final UnitFormatter unitFormatter;
    private boolean animationCompleted = false;

    public RoomCheckPresenter(final SensorResponseInteractor interactor,
                              final UnitFormatter unitFormatter){
        this.interactor = interactor;
        this.unitFormatter = unitFormatter;
    }

    private static final long CONDITION_VISIBLE_MS = 2500;
    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();
    private final List<Sensor> sensors = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.animationCompleted = (savedInstanceState != null);
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        if (animationCompleted) {
            jumpToEnd(false);
        } else {
            view.initialize();
            bindAndSubscribeInteractorLatest();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        jumpToEnd(false);
    }

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
        showConditionAt(0);
    }

    private void unavailableConditions(final Throwable e) {
        sensors.clear();
        view.unavailableConditions(e);
        jumpToEnd(true);
    }

    public void showConditionAt(final int position) {
        if (position >= sensors.size()) {
            jumpToEnd(true);
            return;
        }

        final Sensor currentPositionSensor = sensors.get(position);

        deferWorker.schedule(() -> view.showConditionAt(position,
                                                        currentPositionSensor,
                                                        getSensorConvertedValue(currentPositionSensor),
                                                        unitFormatter.getSuffixForSensor(currentPositionSensor.getType()),
                                                        () -> showConditionAt(position + 1)),
                             CONDITION_VISIBLE_MS,
                             TimeUnit.MILLISECONDS);
    }

    public int getSensorConvertedValue(@NonNull final Sensor sensor){
        final UnitConverter unitConverter = unitFormatter.getUnitConverterForSensor(sensor.getType());
        int convertedValue = 0;
        if (sensor.getValue() != null) {
            convertedValue = (int) unitConverter.convert(sensor.getValue().longValue());
        }
        return convertedValue;
    }

    public Condition calculateAverageCondition(){
        final int conditionCount = sensors.size();
        if (conditionCount > 0) {
            final int conditionSum = Lists.sumInt(sensors, c -> c.getCondition().ordinal());
            final int conditionAverage = (int) Math.ceil(conditionSum / (float) conditionCount);
            final int conditionOrdinal = Math.min(Condition.IDEAL.ordinal(), conditionAverage);

            view.updateSensorView(sensors);

            return Condition.values()[conditionOrdinal];
        }

        return Condition.UNKNOWN;
    }

    public void jumpToEnd(final boolean animate) {
        this.animationCompleted = true;
        deferWorker.unsubscribe();
        execute(() -> {
            view.stopAnimations();
            view.animateSenseCondition(animate, calculateAverageCondition());
            view.showCompletion(animate, bind(() -> view.finishFlow()));
        });

    }

    public interface Output extends BaseOutput {

        void initialize();

        void createSensorConditionViews(List<SensorType> types);

        void unavailableConditions(Throwable e);

        void showConditionAt(int position,
                             Sensor currentPositionSensor,
                             int convertedValue,
                             String unitSuffix,
                             Runnable onComplete);

        void updateSensorView(List<Sensor> sensors);

        void stopAnimations();

        void animateSenseCondition(boolean animate, Condition condition);

        void showCompletion(boolean animate, Runnable onContinue);
    }
}
