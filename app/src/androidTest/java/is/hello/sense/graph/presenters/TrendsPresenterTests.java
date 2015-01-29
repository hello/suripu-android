package is.hello.sense.graph.presenters;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

public class TrendsPresenterTests extends InjectionTestCase {
    @Inject TrendsPresenter trendsPresenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        trendsPresenter.update();
    }


    public void testAvailableTrendGraphs() throws Exception {
        Sync.wrap(trendsPresenter.availableTrendGraphs())
            .assertFalse(Lists::isEmpty);
    }

    public void testUpdateTrend() throws Exception {
        ArrayList<TrendGraph> graphs = Sync.next(trendsPresenter.trends);
        assertEquals(3, graphs.size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_ALL, graphs.get(2).getTimePeriod());

        Sync.last(trendsPresenter.updateTrend(2, TrendGraph.TIME_PERIOD_OVER_TIME_1M));

        ArrayList<TrendGraph> updatedGraphs = Sync.next(trendsPresenter.trends);
        assertEquals(3, updatedGraphs.size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_1M, updatedGraphs.get(2).getTimePeriod());
    }
}
