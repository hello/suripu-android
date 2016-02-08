package is.hello.sense.ui.widget.graphing;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class GridGraphViewTests extends SenseTestCase {
    @Test
    public void requestPopulateCoalesces() {
        final GridGraphView view = spy(new GridGraphView(getContext()));

        final List<Runnable> pending = new ArrayList<>();
        final Answer postAnswer = invocation -> {
            final Runnable task = invocation.getArgumentAt(0, Runnable.class);
            pending.add(task);
            return null;
        };
        doAnswer(postAnswer)
                .when(view)
                .post(any(Runnable.class));

        final Answer removeCallbacksAnswer = invocation -> {
            final Runnable task = invocation.getArgumentAt(0, Runnable.class);
            pending.remove(task);
            return null;
        };
        doAnswer(removeCallbacksAnswer)
                .when(view)
                .removeCallbacks(any(Runnable.class));

        view.requestPopulate();
        view.requestPopulate();
        view.requestPopulate();

        assertThat(pending.size(), is(equalTo(1)));
    }
}
