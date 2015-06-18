package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;
import is.hello.sense.util.markup.text.MarkupString;

import static is.hello.sense.util.ModelHelper.manipulate;

public class TimelinePresenterTests extends InjectionTests {
    @Inject TimelinePresenter presenter;

    @Test
    public void update() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        Sync.wrap(presenter.timeline)
            .assertNotNull();
    }

    @Test
    public void cache() throws Exception {
        Timeline timeline = manipulate(new Timeline())
                .set("date", DateTime.now())
                .set("message", new MarkupString("This is a test"))
                .unwrap();
        presenter.setDateWithTimeline(timeline.getDate(), timeline);

        Sync.wrap(presenter.timeline)
            .assertNotNull();
    }
}
