package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

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
import is.hello.sense.util.Analytics;
import is.hello.sense.util.RoomCheckResMapper;
import rx.Scheduler;

public class RoomCheckPresenter extends BasePresenter<RoomCheckPresenter.Output> {


    private final SensorResponseInteractor interactor;
    private final UnitFormatter unitFormatter;
    private final RoomCheckResMapper mapper;
    private boolean canFinish = false;

    public RoomCheckPresenter(@NonNull final SensorResponseInteractor interactor,
                              @NonNull final UnitFormatter unitFormatter,
                              @NonNull final RoomCheckResMapper mapper){
        this.interactor = interactor;
        this.unitFormatter = unitFormatter;
        this.mapper = mapper;
    }

    private static final long CONDITION_VISIBLE_MS = 2500;
    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();
    private final List<Sensor> sensors = new ArrayList<>();

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.canFinish = (savedInstanceState != null);
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        if (canFinish) {
            jumpToEnd(false);
        } else {
            view.initialize();
            bindAndSubscribe(interactor.latest(),
                             this::bindConditions,
                             this::unavailableConditions
                            );
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

    private void bindConditions(@NonNull final SensorResponse current) {
        sensors.clear();
        sensors.addAll(current.getSensors());
        final int sensorsSize = sensors.size();
        final @DrawableRes int[] types = new int[sensorsSize];
        for(int i=0; i < sensorsSize; i++){
            types[i] = mapper.getInitialIconForSensor(sensors.get(i).getType());
        }
        view.createSensorConditionViews(types);
        showConditionAt(0);
    }

    private void unavailableConditions(@NonNull final Throwable e) {
        Analytics.trackError(e, Analytics.Onboarding.ERROR_MSG_ROOM_CHECK);
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
                () -> {
                    final SensorType type = currentPositionSensor.getType();
                    final Condition condition = currentPositionSensor.getCondition();
                    view.showConditionAt(position,
                                         currentPositionSensor.getMessage(),
                                         mapper.getConditionDrawable(condition),
                                         mapper.getCheckStatusStringForSensor(type),
                                         mapper.getFinalIconForSensor(type),
                                         condition.colorRes,
                                         getSensorConvertedValue(currentPositionSensor),
                                         unitFormatter.getSuffixForSensor(currentPositionSensor.getType()),
                                         () -> showConditionAt(position + 1));
                },
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
        this.canFinish = true;
        deferWorker.unsubscribe();
        execute(() -> {
            view.showCompletion(animate, bind(() -> view.finishFlow()));
        });

    }

    public interface Output extends BaseOutput {

        void initialize();

        void createSensorConditionViews(@DrawableRes int[] icons);

        void unavailableConditions(Throwable e);

        void showConditionAt(int position,
                             String message,
                             @DrawableRes int conditionDrawable,
                             @StringRes int checkStatusMessage,
                             @DrawableRes int conditionIcon,
                             @ColorRes int conditionColorRes,
                             int value,
                             String unitSuffix,
                             Runnable onComplete);

        void showCompletion(boolean animate, Runnable onContinue);
    }
}
