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

    @Override
    public void update() {
        apiService.currentRoomConditions().subscribe(currentConditions::onNext, currentConditions::onError);
    }

}
