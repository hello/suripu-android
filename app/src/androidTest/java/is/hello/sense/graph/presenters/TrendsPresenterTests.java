package is.hello.sense.graph.presenters;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;

import static org.junit.Assert.assertEquals;

public class TrendsPresenterTests extends InjectionTests {
    @Inject TrendsPresenter trendsPresenter;

    @Before
    public void initialize() throws Exception {
        trendsPresenter.update();
    }


    @Test
    public void availableTrendGraphs() throws Exception {
        Sync.wrap(trendsPresenter.availableTrendGraphs())
            .assertFalse(Lists::isEmpty);
    }

    @Test
    public void updateTrend() throws Exception {
        ArrayList<TrendGraph> graphs = Sync.next(trendsPresenter.trends);
        assertEquals(3, graphs.size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_ALL, graphs.get(2).getTimePeriod());

        Sync.last(trendsPresenter.updateTrend(2, TrendGraph.TIME_PERIOD_OVER_TIME_1M));

        ArrayList<TrendGraph> updatedGraphs = Sync.next(trendsPresenter.trends);
        assertEquals(3, updatedGraphs.size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_1M, updatedGraphs.get(2).getTimePeriod());
    }
}
