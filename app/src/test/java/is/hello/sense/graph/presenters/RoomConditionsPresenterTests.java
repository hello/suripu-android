package is.hello.sense.graph.presenters;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static is.hello.sense.graph.presenters.RoomConditionsPresenter.Result;
import static org.junit.Assert.assertNotNull;

public class RoomConditionsPresenterTests extends InjectionTestCase {
    @Inject RoomConditionsPresenter presenter;

    @Test
    public void update() throws Exception {
        Result conditions = Sync.wrapAfter(presenter::update, presenter.currentConditions).last();
        assertNotNull(conditions);
        assertNotNull(conditions.conditions);
        assertNotNull(conditions.units);
        assertNotNull(conditions.roomSensorHistory);
    }
}
