package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

@Singleton public class RoomConditionsPresenter extends ValuePresenter<RoomConditionsPresenter.Result> {
    public static final int HISTORY_HOURS = 2;

    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;

    public final PresenterSubject<Result> currentConditions = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Result> provideUpdateObservable() {
        return preferences.observableUnitSystem().flatMap(unitSystem -> {
            Observable<RoomConditions> roomConditions = apiService.currentRoomConditions(unitSystem.getApiTemperatureUnit());
            Observable<RoomSensorHistory> roomHistory = apiService.roomSensorHistory(HISTORY_HOURS, SensorGraphSample.timeForLatest());
            return Observable.combineLatest(roomConditions, roomHistory,
                    (conditions, history) -> new Result(conditions, history, unitSystem));
        });
    }

    public static final class Result implements Serializable {
        public final RoomConditions conditions;
        public final RoomSensorHistory roomSensorHistory;
        public final UnitSystem units;

        public Result(@NonNull RoomConditions conditions,
                      @NonNull RoomSensorHistory roomSensorHistory,
                      @NonNull UnitSystem units) {
            this.conditions = conditions;
            this.roomSensorHistory = roomSensorHistory;
            this.units = units;
        }
    }
}
