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
import rx.Observable;

@Singleton public class RoomConditionsPresenter extends ValuePresenter<RoomConditionsPresenter.Result> {
    public static final int HISTORY_HOURS = 2;

    @Inject ApiService apiService;

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
        Observable<RoomConditions> roomConditions = apiService.currentRoomConditions(ApiService.UNIT_TEMPERATURE_CELSIUS);
        Observable<RoomSensorHistory> roomHistory = apiService.roomSensorHistory(HISTORY_HOURS, SensorGraphSample.timeForLatest());
        return Observable.combineLatest(roomConditions, roomHistory,
                                        Result::new);
    }

    public static final class Result implements Serializable {
        public final RoomConditions conditions;
        public final RoomSensorHistory roomSensorHistory;

        public Result(@NonNull RoomConditions conditions,
                      @NonNull RoomSensorHistory roomSensorHistory) {
            this.conditions = conditions;
            this.roomSensorHistory = roomSensorHistory;
        }
    }
}
