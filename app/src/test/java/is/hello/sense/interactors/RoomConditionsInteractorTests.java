package is.hello.sense.interactors;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static is.hello.sense.interactors.RoomConditionsInteractor.Result;
import static org.junit.Assert.assertNotNull;

public class RoomConditionsInteractorTests extends InjectionTestCase {
    @Inject
    RoomConditionsInteractor presenter;

    @Test
    public void update() throws Exception {
        Result conditions = Sync.wrapAfter(presenter::update, presenter.currentConditions).last();
        assertNotNull(conditions);
        assertNotNull(conditions.conditions);
        assertNotNull(conditions.roomSensorHistory);
    }
}
