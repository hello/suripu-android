package is.hello.sense.graph.presenters;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import is.hello.sense.util.SyncObserver;

public class InsightsPresenterTests extends InjectionTestCase {
    @Inject InsightsPresenter insightsPresenter;

    public void testUpdate() throws Exception {
        ArrayList<Insight> insights = Sync.wrapAfter(insightsPresenter::update, insightsPresenter.insights).last();
        assertNotNull(insights);
        assertEquals(3, insights.size());
    }
}
