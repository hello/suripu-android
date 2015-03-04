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

        Sync.wrap(presenter.mainTimeline)
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

        Sync.wrap(presenter.mainTimeline)
            .assertNotNull();

        Sync.wrap(presenter.renderedTimelineMessage.take(1))
            .assertFalse(TextUtils::isEmpty);
    }
}
