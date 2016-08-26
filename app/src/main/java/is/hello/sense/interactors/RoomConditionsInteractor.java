package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.units.UnitFormatter;
import rx.Observable;

@Singleton public class RoomConditionsInteractor extends ValueInteractor<RoomConditionsInteractor.Result> {
    public static final int HISTORY_HOURS = 2;

    @Inject
    PreferencesInteractor preferences;
    @Inject ApiService apiService;

    public final InteractorSubject<Result> currentConditions = this.subject;

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
        final boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        final boolean useCelsius = preferences.getBoolean(PreferencesInteractor.USE_CELSIUS,
                                                          defaultMetric);
        final String temperatureUnit = useCelsius
                ? ApiService.UNIT_TEMPERATURE_CELSIUS
                : ApiService.UNIT_TEMPERATURE_US_CUSTOMARY;

        final Observable<RoomConditions> roomConditions =
                apiService.currentRoomConditions(temperatureUnit);
        final Observable<RoomSensorHistory> roomHistory =
                apiService.roomSensorHistory(HISTORY_HOURS, SensorGraphSample.timeForLatest());

        return Observable.combineLatest(roomConditions,
                                        roomHistory,
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
