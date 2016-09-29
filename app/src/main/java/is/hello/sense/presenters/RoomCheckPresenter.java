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

    private void bindAndSubscribeInteractorLatest() {
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
            deferWorker.schedule(() -> jumpToEnd(true),
                                 CONDITION_VISIBLE_MS,
                                 TimeUnit.MILLISECONDS);
            return;
        }

        final Sensor currentPositionSensor = sensors.get(position);

        deferWorker.schedule(
                () -> view.showConditionAt(position,
                                           currentPositionSensor.getMessage(),
                                           currentPositionSensor.getCondition(),
                                           currentPositionSensor.getType(),
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

    public void jumpToEnd(final boolean animate) {
        this.animationCompleted = true;
        deferWorker.unsubscribe();
        execute(() -> {
            view.showCompletion(animate, bind(() -> view.finishFlow()));
        });

    }

    public interface Output extends BaseOutput {

        void initialize();

        void createSensorConditionViews(List<SensorType> types);

        void unavailableConditions(Throwable e);

        void showConditionAt(int position,
                             String message,
                             Condition condition,
                             SensorType type,
                             int convertedValue,
                             String unitSuffix,
                             Runnable onComplete);

        void showCompletion(boolean animate, Runnable onContinue);
    }
}
