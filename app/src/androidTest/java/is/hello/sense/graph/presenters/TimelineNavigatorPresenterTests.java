package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

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
        assertEquals(startTime.plusDays(-5), presenter.getDateTimeAt(5));
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

        Observable<Timeline> timeline = presenter.timelineForDate(startTime);
        SyncObserver<Timeline> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, timeline);
        observer.await();

        assertNotNull(observer.getError());
        assertTrue(observer.getError() instanceof IllegalStateException);

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

    public void testTimelineForDate() throws Exception {
        Observable<Timeline> firstTimeline = presenter.timelineForDate(startTime);
        SyncObserver<Timeline> firstObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, firstTimeline);
        firstObserver.await();

        assertNull(firstObserver.getError());
        assertNotNull(firstObserver.getSingle());
        assertEquals(77, firstObserver.getSingle().getScore());


        Observable<Timeline> secondTimeline = presenter.timelineForDate(startTime);
        SyncObserver<Timeline> secondObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, secondTimeline);
        secondObserver.await();

        assertNull(secondObserver.getError());
        assertNotNull(secondObserver.getSingle());
        assertEquals(77, secondObserver.getSingle().getScore());
        assertSame(secondObserver.getSingle(), firstObserver.getSingle());
    }
}
