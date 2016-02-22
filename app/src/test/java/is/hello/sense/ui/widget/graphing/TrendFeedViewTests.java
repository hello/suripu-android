package is.hello.sense.ui.widget.graphing;

import android.view.View;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class TrendFeedViewTests extends SenseTestCase {
    private final AnimatorContext animatorContext = new AnimatorContext("TrendFeedViewTests");
    private TrendFeedView view;

    @Before
    public void setUp() {
        this.view = new TrendFeedView(getContext());
        view.setAnimatorContext(animatorContext);
    }

    @Test
    public void bindMoreTrends() {
        final Graph graph1 = new Graph("Test 1", Graph.DataType.HOURS, Graph.GraphType.BAR);
        final Trends oneTrend = new Trends(Collections.emptyList(),
                                           Lists.newArrayList(graph1));
        view.bindTrends(oneTrend);

        assertThat(view.getChildCount(), is(equalTo(1)));
        final View firstChild = view.getChildAt(0);

        final Graph graph2 = new Graph("Test 2", Graph.DataType.HOURS, Graph.GraphType.BUBBLES);
        final Trends twoTrends = new Trends(Collections.emptyList(),
                                            Lists.newArrayList(graph1, graph2));
        view.bindTrends(twoTrends);

        assertThat(view.getChildCount(), is(equalTo(2)));
        assertThat(view.getChildAt(0), is(sameInstance(firstChild)));
    }

    @Test
    public void bindLessTrends() {
        final Graph graph1 = new Graph("Test 1", Graph.DataType.HOURS, Graph.GraphType.BAR);
        final Graph graph2 = new Graph("Test 2", Graph.DataType.HOURS, Graph.GraphType.BUBBLES);
        final Trends twoTrends = new Trends(Collections.emptyList(),
                                            Lists.newArrayList(graph1, graph2));
        view.bindTrends(twoTrends);

        assertThat(view.getChildCount(), is(equalTo(2)));
        final View firstChild = view.getChildAt(0);

        final Trends oneTrend = new Trends(Collections.emptyList(),
                                           Lists.newArrayList(graph1));
        view.bindTrends(oneTrend);

        assertThat(view.getChildCount(), is(equalTo(1)));
        assertThat(view.getChildAt(0), is(sameInstance(firstChild)));
    }

    @Test
    public void errors() {
        view.presentError(() -> {});

        assertThat(view.getChildCount(), is(equalTo(1)));
        final View errorView = view.getChildAt(0);

        final Graph graph = new Graph("Test 1", Graph.DataType.HOURS, Graph.GraphType.BAR);
        final Trends trends = new Trends(Collections.emptyList(),
                                         Lists.newArrayList(graph));
        view.bindTrends(trends);

        assertThat(view.getChildCount(), is(equalTo(1)));
        assertThat(view.getChildAt(0), is(not(sameInstance(errorView))));
        assertThat(errorView.getParent(), is(nullValue()));
    }
}