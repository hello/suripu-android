package is.hello.sense.graph.presenters;

import android.text.TextUtils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

public class TimelinePresenterTests extends InjectionTestCase {
    @Inject TimelinePresenter presenter;

    public void testUpdate() throws Exception {
        presenter.setDate(DateTime.now());

        assertFalse(Lists.isEmpty(Sync.next(presenter.timeline)));
        assertNotNull(Sync.last(presenter.mainTimeline.take(1)));
        assertFalse(TextUtils.isEmpty(Sync.last(presenter.renderedTimelineMessage.take(1))));
    }

    public void testMemoryPressure() throws Exception {
        presenter.setDate(DateTime.now());
        assertFalse(Lists.isEmpty(Sync.next(presenter.timeline)));

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);

        assertEquals(Collections.<Timeline>emptyList(), Sync.next(presenter.timeline));
        assertNull(Sync.last(presenter.mainTimeline.take(1)));
        assertTrue(TextUtils.isEmpty(Sync.last(presenter.renderedTimelineMessage.take(1))));

        presenter.onContainerResumed();

        assertFalse(Lists.isEmpty(Sync.next(presenter.timeline)));
    }
}
