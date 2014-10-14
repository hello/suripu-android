package is.hello.sense.graph.presenters;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.subjects.ReplaySubject;

@Singleton public class CurrentConditionsPresenter extends Presenter {
    @Inject ApiService apiService;

    public final ReplaySubject<RoomConditions> currentConditions = ReplaySubject.create(1);

    public final Observable<SensorState> temperature = currentConditions.filter(Functions::isNotNull)
                                                                        .map(RoomConditions::getTemperature);
    public final Observable<SensorState> humidity = currentConditions.filter(Functions::isNotNull)
                                                                     .map(RoomConditions::getHumidity);
    public final Observable<SensorState> particulates = currentConditions.filter(Functions::isNotNull)
                                                                         .map(RoomConditions::getParticulates);

    @Override
    public void update() {
        apiService.currentRoomConditions().subscribe(currentConditions::onNext, currentConditions::onError);
    }

}
