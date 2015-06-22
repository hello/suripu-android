package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.Sync;
import is.hello.sense.util.markup.text.MarkupString;

import static is.hello.sense.util.ModelHelper.manipulate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ZoomedOutTimelinePresenterTests extends InjectionTests {
    private final DateTime startTime = DateTime.now();

    @Inject ZoomedOutTimelinePresenter presenter;

    @Before
    public void initialize() throws Exception {
        presenter.setFirstDate(startTime);
    }

    @Test
    public void getDateTimeAt() throws Exception {
        assertEquals(startTime.plusDays(-5).withTimeAtStartOfDay(), presenter.getDateTimeAt(5));
    }

    @Test
    public void lowMemoryResponse() throws Exception {
        presenter.getCachedTimelines().put(startTime, new Timeline());
        assertEquals(1, presenter.getCachedTimelines().size());

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        assertEquals(0, presenter.getCachedTimelines().size());
    }

    @Test
    public void postResumed() throws Exception {
        LambdaVar<Boolean> called = LambdaVar.of(false);
        presenter.post("tag", () -> called.set(true));
        assertTrue(called.get());
    }

    @Test
    public void postSuspended() throws Exception {
        LambdaVar<Boolean> called = LambdaVar.of(false);
        presenter.suspend();
        presenter.post("tag", () -> called.set(true));
        assertFalse(called.get());

        Sync.wrap(presenter.timelineForDate(startTime))
            .assertThrows(IllegalStateException.class);
        presenter.resume();

        assertTrue(called.get());
    }

    @Test
    public void postCancel() throws Exception {
        LambdaVar<Boolean> called = LambdaVar.of(false);

        presenter.suspend();
        presenter.post("tag", () -> called.set(true));
        assertFalse(called.get());

        presenter.cancel("tag");

        presenter.resume();
        assertFalse(called.get());
    }

    @Test
    public void cacheInjection() throws Exception {
        Timeline timeline = manipulate(new Timeline())
                .set("date", DateTime.now())
                .set("message", new MarkupString("This is a test"))
                .unwrap();

        presenter.cacheSingleTimeline(timeline.getDate(), timeline);

        Timeline presenterTimeline = Sync.last(presenter.timelineForDate(timeline.getDate()));
        assertNotNull(presenterTimeline);
        assertSame(timeline, presenterTimeline);
    }

    @Test
    public void timelineForDate() throws Exception {
        Timeline firstTimeline = Sync.last(presenter.timelineForDate(startTime));
        assertNotNull(firstTimeline);
        assertEquals(77, firstTimeline.getScore());

        Timeline secondTimeline = Sync.last(presenter.timelineForDate(startTime));
        assertNotNull(secondTimeline);
        assertEquals(77, secondTimeline.getScore());
        assertSame(secondTimeline, firstTimeline);
    }
}
