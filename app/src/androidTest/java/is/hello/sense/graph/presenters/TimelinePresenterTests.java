package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

public class TimelinePresenterTests extends InjectionTestCase {
    @Inject TimelinePresenter presenter;

    public void testUpdate() throws Exception {
        Observable<List<Timeline>> fullTimeline = presenter.timeline;
        presenter.setDate(DateTime.now());

        SyncObserver<List<Timeline>> allObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, fullTimeline);
        allObserver.await();

        assertNull(allObserver.getError());
        assertNotNull(allObserver.getSingle());


        SyncObserver<Timeline> mainObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.mainTimeline);
        mainObserver.await();

        assertNull(mainObserver.getError());
        assertNotNull(mainObserver.getSingle());


        SyncObserver<CharSequence> messageObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.renderedTimelineMessage);
        messageObserver.await();

        assertNull(messageObserver.getError());
        assertNotNull(messageObserver.getSingle());
    }
}
