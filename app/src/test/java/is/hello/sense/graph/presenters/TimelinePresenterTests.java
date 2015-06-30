package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineBuilder;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.TimelineEventBuilder;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;
import is.hello.sense.util.markup.text.MarkupString;

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
        Timeline timeline = new TimelineBuilder()
                .setDate(DateTime.now())
                .setMessage(new MarkupString("This is a test"))
                .build();
        presenter.setDateWithTimeline(timeline.getDate(), timeline);

        Sync.wrap(presenter.timeline)
            .assertNotNull();
    }

    @Test
    public void amendEventTime() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        TimelineEvent timelineEvent = new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GOT_IN_BED)
                .build();

        Sync.wrap(presenter.amendEventTime(timelineEvent, LocalTime.now()))
            .assertNotNull();
    }

    @Test
    public void verifyEvent() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        TimelineEvent timelineEvent = new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .build();

        Sync.wrap(presenter.verifyEvent(timelineEvent))
            .assertNotNull();
    }

    @Test
    public void deleteEvent() throws Exception {
        presenter.setDateWithTimeline(DateTime.now(), null);

        TimelineEvent timelineEvent = new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .build();

        Sync.wrap(presenter.deleteEvent(timelineEvent))
            .assertNotNull();
    }
}
