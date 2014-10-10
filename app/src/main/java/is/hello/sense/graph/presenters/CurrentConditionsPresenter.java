package is.hello.sense.graph.presenters;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.RoomConditions;
import rx.subjects.ReplaySubject;

@Singleton public class CurrentConditionsPresenter extends Presenter {
    @Inject ApiService apiService;

    public final ReplaySubject<RoomConditions> currentConditions = ReplaySubject.create(1);

    public CurrentConditionsPresenter() {
    }

    @Override
    public void update() {
        apiService.currentRoomConditions().subscribe(currentConditions::onNext, currentConditions::onError);
    }

}
