package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import is.hello.sense.util.markup.text.MarkupString;

import static is.hello.sense.util.ModelHelper.manipulate;

public class TimelinePresenterTests extends InjectionTestCase {
    @Inject TimelinePresenter presenter;

    public void testUpdate() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        Sync.wrap(presenter.timeline)
            .assertNotNull();
    }

    public void testCache() throws Exception {
        Timeline timeline = manipulate(new Timeline())
                .set("date", DateTime.now())
                .set("message", new MarkupString("This is a test"))
                .unwrap();
        presenter.setDateWithTimeline(timeline.getDate(), timeline);

        Sync.wrap(presenter.timeline)
            .assertNotNull();
    }
}
