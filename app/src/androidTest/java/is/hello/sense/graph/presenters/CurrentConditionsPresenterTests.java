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
    }
}
