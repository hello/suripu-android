package is.hello.sense.api.model.v2;

import org.junit.Test;

import is.hello.sense.api.model.Condition;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GraphTests extends SenseTestCase {
    @Test
    public void getConditionForValue() {
        final Graph graph = new Graph();
        graph.conditionRanges = Lists.newArrayList(new ConditionRange(0, 59, Condition.ALERT),
                                                   new ConditionRange(60, 79, Condition.WARNING),
                                                   new ConditionRange(80, 100, Condition.IDEAL));

        assertThat(graph.getConditionForValue(30f), is(equalTo(Condition.ALERT)));
        assertThat(graph.getConditionForValue(70f), is(equalTo(Condition.WARNING)));
        assertThat(graph.getConditionForValue(90f), is(equalTo(Condition.IDEAL)));
        assertThat(graph.getConditionForValue(-20f), is(equalTo(Condition.UNKNOWN)));
        assertThat(graph.getConditionForValue(120f), is(equalTo(Condition.UNKNOWN)));
    }
}
