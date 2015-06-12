package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.Sync;
import is.hello.sense.util.markup.text.MarkupString;

import static is.hello.sense.util.ModelHelper.manipulate;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class TimelineNavigatorPresenterTests extends InjectionTestCase {
    private final DateTime startTime = DateTime.now();

    @Inject TimelineNavigatorPresenter presenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        presenter.setFirstDate(startTime);
    }

    public void testGetDateTimeAt() throws Exception {
        assertEquals(startTime.plusDays(-5).withTimeAtStartOfDay(), presenter.getDateTimeAt(5));
    }

    public void testLowMemoryResponse() throws Exception {
        presenter.getCachedTimelines().put(startTime, new Timeline());
        assertEquals(1, presenter.getCachedTimelines().size());

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        assertEquals(0, presenter.getCachedTimelines().size());
    }

    public void testPostResumed() throws Exception {
        LambdaVar<Boolean> called = LambdaVar.of(false);
        presenter.post("tag", () -> called.set(true));
        assertTrue(called.get());
    }

    public void testPostSuspended() throws Exception {
        LambdaVar<Boolean> called = LambdaVar.of(false);
        presenter.suspend();
        presenter.post("tag", () -> called.set(true));
        assertFalse(called.get());

        Sync.wrap(presenter.timelineForDate(startTime))
            .assertThrows(IllegalStateException.class);
        presenter.resume();

        assertTrue(called.get());
    }

    public void testPostCancel() throws Exception {
        LambdaVar<Boolean> called = LambdaVar.of(false);

        presenter.suspend();
        presenter.post("tag", () -> called.set(true));
        assertFalse(called.get());

        presenter.cancel("tag");

        presenter.resume();
        assertFalse(called.get());
    }

    public void testCacheInjection() throws Exception {
        Timeline timeline = manipulate(new Timeline())
                .set("date", DateTime.now())
                .set("message", new MarkupString("This is a test"))
                .unwrap();

        presenter.cacheSingleTimeline(timeline.getDate(), timeline);

        Timeline presenterTimeline = Sync.last(presenter.timelineForDate(timeline.getDate()));
        assertNotNull(presenterTimeline);
        assertSame(timeline, presenterTimeline);
    }

    public void testTimelineForDate() throws Exception {
        Timeline firstTimeline = Sync.last(presenter.timelineForDate(startTime));
        assertNotNull(firstTimeline);
        assertEquals(77, firstTimeline.getScore());

        Timeline secondTimeline = Sync.last(presenter.timelineForDate(startTime));
        assertNotNull(secondTimeline);
        assertEquals(77, secondTimeline.getScore());
        assertSame(secondTimeline, firstTimeline);
    }
}
