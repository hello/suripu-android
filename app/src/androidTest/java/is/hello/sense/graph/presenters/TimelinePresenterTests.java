package is.hello.sense.graph.presenters;

import android.text.TextUtils;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static is.hello.sense.util.ModelHelper.manipulate;

public class TimelinePresenterTests extends InjectionTestCase {
    @Inject TimelinePresenter presenter;

    public void testUpdate() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        Sync.wrap(presenter.timeline)
            .assertFalse(Lists::isEmpty);

        Sync.wrap(presenter.mainTimeline.take(1))
            .assertNotNull();

        Sync.wrap(presenter.renderedTimelineMessage.take(1))
            .assertFalse(TextUtils::isEmpty);
    }

    public void testCache() throws Exception {
        Timeline timeline = manipulate(new Timeline())
                .set("date", DateTime.now())
                .set("message", "This is a test")
                .unwrap();
        presenter.setDateWithTimeline(timeline.getDate(), timeline);

        Sync.wrap(presenter.timeline)
            .assertFalse(Lists::isEmpty);

        Sync.wrap(presenter.mainTimeline.take(1))
            .assertNotNull();

        Sync.wrap(presenter.renderedTimelineMessage.take(1))
            .assertFalse(TextUtils::isEmpty);
    }

    public void testMemoryPressure() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        Sync.wrap(presenter.timeline)
            .assertFalse(Lists::isEmpty);

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);

        Sync.wrap(presenter.timeline)
            .assertTrue(Lists::isEmpty);
        Sync.wrap(presenter.mainTimeline.take(1))
            .assertNull();
        Sync.wrap(presenter.renderedTimelineMessage.take(1))
            .assertTrue(TextUtils::isEmpty);

        presenter.onContainerResumed();

        Sync.wrap(presenter.timeline)
            .assertFalse(Lists::isEmpty);
    }
}
