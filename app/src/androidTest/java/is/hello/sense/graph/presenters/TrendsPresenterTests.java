package is.hello.sense.graph.presenters;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.AvailableTrendGraph;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class TrendsPresenterTests extends InjectionTestCase {
    @Inject TrendsPresenter trendsPresenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        trendsPresenter.update();
    }


    public void testAvailableTrendGraphs() throws Exception {
        SyncObserver<ArrayList<AvailableTrendGraph>> availableGraphs = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, trendsPresenter.availableTrendGraphs());
        availableGraphs.await();

        assertNull(availableGraphs.getError());
        assertNotNull(availableGraphs.getSingle());
        assertFalse(availableGraphs.getSingle().isEmpty());
    }

    public void testUpdateTrend() throws Exception {
        SyncObserver<ArrayList<TrendGraph>> graphs = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, trendsPresenter.trends);
        graphs.await();

        assertNull(graphs.getError());
        assertNotNull(graphs.getSingle());
        assertEquals(3, graphs.getSingle().size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_ALL, graphs.getSingle().get(2).getTimePeriod());


        SyncObserver<Void> update = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, trendsPresenter.updateTrend(2, TrendGraph.TIME_PERIOD_OVER_TIME_1M));
        update.await();

        assertNull(graphs.getError());


        graphs.reset().subscribeTo(trendsPresenter.trends);
        graphs.await();

        assertNull(graphs.getError());
        assertNotNull(graphs.getSingle());
        assertEquals(3, graphs.getSingle().size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_1M, graphs.getSingle().get(2).getTimePeriod());
    }
}
