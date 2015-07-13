package is.hello.sense.graph.presenters;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TrendsPresenterTests extends InjectionTestCase {
    @Inject TrendsPresenter trendsPresenter;

    @Before
    public void initialize() throws Exception {
        trendsPresenter.update();
    }


    @Test
    public void renderEmptyTrendGraph() throws Exception {
        TrendGraph trendGraph = new TrendGraph.Builder()
                .setTitle("Test Graph")
                .setDataType(TrendGraph.DataType.SLEEP_SCORE)
                .setTimePeriod(TrendGraph.TIME_PERIOD_DAY_OF_WEEK)
                .build();

        TrendsPresenter.Rendered rendered = TrendsPresenter.renderTrendGraph(trendGraph);
        assertNotNull(rendered.graph);
        assertNotNull(rendered.sectionSamples);
        assertNull(rendered.extremes);
    }

    @Test
    public void renderTrendGraphSingleSample() throws Exception {
        TrendGraph trendGraph = new TrendGraph.Builder()
                .setTitle("Test Graph")
                .setDataType(TrendGraph.DataType.SLEEP_SCORE)
                .setTimePeriod(TrendGraph.TIME_PERIOD_DAY_OF_WEEK)
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(30f)
                        .build())
                .build();

        TrendsPresenter.Rendered rendered = TrendsPresenter.renderTrendGraph(trendGraph);

        assertNotNull(rendered.graph);
        assertEquals(trendGraph, rendered.graph);

        assertNotNull(rendered.sectionSamples);

        assertNotNull(rendered.extremes);
        assertEquals(ApiService.PLACEHOLDER_VALUE, rendered.extremes.minPosition);
        assertEquals(0f, rendered.extremes.minValue, 0f);
        assertEquals(ApiService.PLACEHOLDER_VALUE, rendered.extremes.maxPosition);
        assertEquals(30f, rendered.extremes.maxValue, 0f);
    }

    @Test
    public void renderTrendGraphMultipleSamplesAllTime() throws Exception {
        TrendGraph trendGraph = new TrendGraph.Builder()
                .setTitle("Test Graph")
                .setDataType(TrendGraph.DataType.SLEEP_SCORE)
                .setTimePeriod(TrendGraph.TIME_PERIOD_OVER_TIME_ALL)
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(10f)
                        .build())
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(30f)
                        .build())
                .duplicateDataPoint(1, 50)
                .build();

        TrendsPresenter.Rendered rendered = TrendsPresenter.renderTrendGraph(trendGraph);

        assertNotNull(rendered.graph);
        assertEquals(trendGraph, rendered.graph);

        assertNotNull(rendered.sectionSamples);
        assertTrue(Lists.isEmpty(rendered.sectionSamples));

        assertNotNull(rendered.extremes);
        assertEquals(0, rendered.extremes.minPosition);
        assertEquals(10f, rendered.extremes.minValue, 0f);
        assertEquals(1, rendered.extremes.maxPosition);
        assertEquals(30f, rendered.extremes.maxValue, 0f);
    }

    @Test
    public void renderTrendGraphForMonths() throws Exception {
        TrendGraph trendGraph = new TrendGraph.Builder()
                .setTitle("Test Graph")
                .setDataType(TrendGraph.DataType.SLEEP_SCORE)
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(10f)
                        .build())
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(30f)
                        .build())
                .duplicateDataPoint(1, 60)
                .build();

        TrendsPresenter.Rendered rendered = TrendsPresenter.renderTrendGraph(trendGraph);

        assertNotNull(rendered.graph);
        assertEquals(trendGraph, rendered.graph);

        assertNotNull(rendered.sectionSamples);
        assertFalse(Lists.isEmpty(rendered.sectionSamples));
        assertEquals(3, rendered.sectionSamples.size());

        assertNotNull(rendered.extremes);
        assertEquals(0, rendered.extremes.minPosition);
        assertEquals(10f, rendered.extremes.minValue, 0f);
        assertEquals(1, rendered.extremes.maxPosition);
        assertEquals(30f, rendered.extremes.maxValue, 0f);
    }

    @Test
    public void renderTrendGraphForDays() throws Exception {
        TrendGraph trendGraph1 = new TrendGraph.Builder()
                .setTitle("Test Graph")
                .setDataType(TrendGraph.DataType.SLEEP_SCORE)
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(10f)
                        .build())
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(30f)
                        .build())
                .duplicateDataPoint(1, 8)
                .build();

        TrendsPresenter.Rendered rendered1 = TrendsPresenter.renderTrendGraph(trendGraph1);

        assertNotNull(rendered1.graph);
        assertEquals(trendGraph1, rendered1.graph);

        assertNotNull(rendered1.sectionSamples);
        assertFalse(Lists.isEmpty(rendered1.sectionSamples));
        assertEquals(2, rendered1.sectionSamples.size());

        assertNotNull(rendered1.extremes);
        assertEquals(0, rendered1.extremes.minPosition);
        assertEquals(10f, rendered1.extremes.minValue, 0f);
        assertEquals(1, rendered1.extremes.maxPosition);
        assertEquals(30f, rendered1.extremes.maxValue, 0f);


        TrendGraph trendGraph2 = new TrendGraph.Builder()
                .setTitle("Test Graph")
                .setDataType(TrendGraph.DataType.SLEEP_SCORE)
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(10f)
                        .build())
                .addDataPoint(new TrendGraph.GraphSample.Builder()
                        .setDataLabel(TrendGraph.DataLabel.GOOD)
                        .setYValue(30f)
                        .build())
                .duplicateDataPoint(1, 5)
                .build();

        TrendsPresenter.Rendered rendered2 = TrendsPresenter.renderTrendGraph(trendGraph2);

        assertNotNull(rendered2.graph);
        assertEquals(trendGraph2, rendered2.graph);

        assertNotNull(rendered2.sectionSamples);
        assertFalse(Lists.isEmpty(rendered2.sectionSamples));
        assertEquals(7, rendered2.sectionSamples.size());

        assertNotNull(rendered2.extremes);
        assertEquals(0, rendered2.extremes.minPosition);
        assertEquals(10f, rendered2.extremes.minValue, 0f);
        assertEquals(1, rendered2.extremes.maxPosition);
        assertEquals(30f, rendered2.extremes.maxValue, 0f);
    }

    @Test
    public void availableTrendGraphs() throws Exception {
        Sync.wrap(trendsPresenter.availableTrendGraphs())
            .assertFalse(Lists::isEmpty);
    }

    @Test
    public void updateTrend() throws Exception {
        ArrayList<TrendsPresenter.Rendered> renderedGraphs = Sync.next(trendsPresenter.trends);
        assertEquals(3, renderedGraphs.size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_ALL, renderedGraphs.get(2).graph.getTimePeriod());

        Sync.last(trendsPresenter.updateTrend(2, TrendGraph.TIME_PERIOD_OVER_TIME_1M));

        ArrayList<TrendsPresenter.Rendered> updatedGraphs = Sync.next(trendsPresenter.trends);
        assertEquals(3, updatedGraphs.size());
        assertEquals(TrendGraph.TIME_PERIOD_OVER_TIME_1M, updatedGraphs.get(2).graph.getTimePeriod());
    }
}
