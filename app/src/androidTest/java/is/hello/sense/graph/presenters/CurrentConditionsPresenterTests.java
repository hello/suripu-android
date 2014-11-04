package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class CurrentConditionsPresenterTests extends InjectionTestCase {
    @Inject CurrentConditionsPresenter presenter;

    public void testUpdate() throws Exception {
        SyncObserver<CurrentConditionsPresenter.Result> conditions = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.currentConditions);
        presenter.update();
        conditions.await();

        assertNull(conditions.getError());
        assertNotNull(conditions.getSingle());
        assertNotNull(conditions.getSingle().conditions);
        assertNotNull(conditions.getSingle().units);
    }
}
