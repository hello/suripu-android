package is.hello.sense.flows.home.ui.adapters;

import android.widget.LinearLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;

import static junit.framework.Assert.assertEquals;

public class TrendsAdapterTest extends SenseTestCase {
    private TrendsAdapter trendsAdapter;

    @Before
    public void setUp() {
        this.trendsAdapter = new TrendsAdapter(Mockito.mock(AnimatorContext.class),
                                               Mockito.mock(TrendGraphView.AnimationCallback.class),
                                               Mockito.mock(TrendFeedViewItem.OnRetry.class),
                                               true);
    }

    @Test
    public void testNoGraphs() {
        final Trends noGraphTrends = new Trends(new ArrayList<>(), new ArrayList<>());
        trendsAdapter.setTrends(noGraphTrends);
        assertEquals(trendsAdapter.getItemCount(), 1);
        assertEquals(trendsAdapter.getItemViewType(0), 0);
    }

    @Test
    public void testThreeGraphs() {
        final Trends threeGraphTrends = new Trends(new ArrayList<>(), getThreeGraphs());
        trendsAdapter.setTrends(threeGraphTrends);
        assertEquals(trendsAdapter.getItemCount(), 3);
        assertEquals(trendsAdapter.getItemViewType(0), 1);
        assertEquals(trendsAdapter.getItemViewType(1), 1);
        assertEquals(trendsAdapter.getItemViewType(2), 1);
    }

    @Test
    public void testError() {
        trendsAdapter.showError();
        assertEquals(trendsAdapter.getItemCount(), 1);
        assertEquals(trendsAdapter.getItemViewType(0), 2);
    }


    private List<Graph> getThreeGraphs() {
        final List<Graph> threeGraphs = new ArrayList<>();
        threeGraphs.add(quickGridGraph());
        threeGraphs.add(quickBarGraph());
        threeGraphs.add(quickBubbleGraph());
        return threeGraphs;
    }

    private Graph quickGridGraph() {
        return new Graph("Grid", Graph.DataType.HOURS, Graph.GraphType.GRID);
    }

    private Graph quickBarGraph() {
        return new Graph("Bar", Graph.DataType.HOURS, Graph.GraphType.BAR);
    }

    private Graph quickBubbleGraph() {
        return new Graph("Bubble", Graph.DataType.HOURS, Graph.GraphType.BUBBLES);
    }
}
