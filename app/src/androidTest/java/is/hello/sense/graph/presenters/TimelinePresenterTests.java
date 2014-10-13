package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

public class TimelinePresenterTests extends InjectionTestCase {
    @Inject TimelinePresenter timelinePresenter;

    public void testUpdate() throws Exception {
        Observable<List<Timeline>> fullTimeline = timelinePresenter.timeline;
        timelinePresenter.setDate(DateTime.now());

        SyncObserver<List<Timeline>> allObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, fullTimeline);
        allObserver.await();

        assertNull(allObserver.getError());
        assertFalse(allObserver.getResults().isEmpty());
        assertEquals(1, allObserver.getResults().size());


        SyncObserver<Timeline> mainObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, timelinePresenter.mainTimeline);
        mainObserver.await();

        assertNull(mainObserver.getError());
        assertFalse(mainObserver.getResults().isEmpty());
        assertNotNull(mainObserver.getResults().get(0));


        SyncObserver<CharSequence> messageObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, timelinePresenter.renderedTimelineMessage);
        messageObserver.await();

        assertNull(messageObserver.getError());
        assertFalse(messageObserver.getResults().isEmpty());
        assertNotNull(messageObserver.getResults().get(0));
    }
}
