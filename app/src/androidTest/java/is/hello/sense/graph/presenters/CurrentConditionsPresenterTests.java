package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class CurrentConditionsPresenterTests extends InjectionTestCase {
    @Inject CurrentConditionsPresenter presenter;

    public void testUpdate() throws Exception {
        SyncObserver<RoomConditions> conditions = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.currentConditions);
        presenter.update();
        conditions.await();

        assertNull(conditions.getError());
        assertNotNull(conditions.getSingle());


        SyncObserver<SensorState> temperature = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.temperature);
        temperature.await();

        assertNull(temperature.getError());
        assertNotNull(temperature.getSingle());


        SyncObserver<SensorState> humidity = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.humidity);
        humidity.await();

        assertNull(humidity.getError());
        assertNotNull(humidity.getSingle());


        SyncObserver<SensorState> particulates = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.particulates);
        particulates.await();

        assertNull(particulates.getError());
        assertNotNull(particulates.getSingle());
    }
}
