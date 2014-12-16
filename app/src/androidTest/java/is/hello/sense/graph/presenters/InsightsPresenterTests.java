package is.hello.sense.graph.presenters;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class InsightsPresenterTests extends InjectionTestCase {
    @Inject InsightsPresenter insightsPresenter;

    public void testUpdate() throws Exception {
        insightsPresenter.update();

        SyncObserver<ArrayList<Insight>> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, insightsPresenter.insights);
        observer.await();

        assertNull(observer.getError());
        assertNotNull(observer.getSingle());
        assertEquals(3, observer.getSingle().size());
    }
}
