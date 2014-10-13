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

        SyncObserver<List<Timeline>> observer = SyncObserver.waitOn(SyncObserver.WaitingFor.NEXT, fullTimeline);
        observer.await();

        assertNull(observer.getError());
        assertFalse(observer.getResults().isEmpty());
    }
}
