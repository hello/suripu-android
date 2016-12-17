package is.hello.sense.interactors;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineBuilder;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.TimelineEventBuilder;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import is.hello.sense.util.markup.text.MarkupString;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

public class TimelineInteractorTests extends InjectionTestCase {
    @Inject
    TimelineInteractor presenter;

    @Before
    public void setUp() throws Exception {
        presenter.clearCache();
    }

    @Test
    public void update() throws Exception {
        presenter.setDateWithTimeline(LocalDate.now(), null);
        presenter.update();

        Sync.wrap(presenter.timeline)
            .assertThat(notNullValue());
    }

    @Test
    public void cache() throws Exception {
        Timeline timeline = new TimelineBuilder()
                .setDate(LocalDate.now())
                .setMessage(new MarkupString("This is a test"))
                .build();
        presenter.setDateWithTimeline(timeline.getDate(), timeline);

        Sync.wrap(presenter.timeline)
            .assertThat(notNullValue());
    }

    @Test
    public void amendEventTime() throws Exception {
        presenter.setDateWithTimeline(LocalDate.now(), null);
        presenter.update();

        TimelineEvent timelineEvent = new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GOT_IN_BED)
                .build();

        Sync.wrap(presenter.amendEventTime(timelineEvent, LocalTime.now()))
            .assertThat(nullValue());

        Timeline timeline = Sync.next(presenter.timeline);
        assertFalse(Lists.isEmpty(timeline.getEvents()));
    }

    @Test
    public void verifyEvent() throws Exception {
        presenter.setDateWithTimeline(LocalDate.now(), null);
        presenter.update();

        TimelineEvent timelineEvent = new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .build();

        Sync.wrap(presenter.verifyEvent(timelineEvent))
            .assertThat(nullValue());
    }

    @Test
    public void deleteEvent() throws Exception {
        presenter.setDateWithTimeline(LocalDate.now(), null);
        presenter.update();

        TimelineEvent timelineEvent = new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .build();

        Sync.wrap(presenter.deleteEvent(timelineEvent))
            .assertThat(nullValue());

        Timeline timeline = Sync.next(presenter.timeline);
        assertFalse(Lists.isEmpty(timeline.getEvents()));
    }

    @Test
    public void validTimelineCache() throws Exception {
        final Timeline validTimeline = new TimelineBuilder()
                .setDate(LocalDate.now())
                .setScore(82, ScoreCondition.IDEAL)
                .setMessage(new MarkupString("This is a test"))
                .build();

        presenter.setDateWithTimeline(validTimeline.getDate(), validTimeline);

        assertTrue(presenter.hasValidTimeline());
        assertTrue(presenter.hasValidTimeline(validTimeline.getDate()));
    }

    @Test
    public void invalidTimelineCache() throws Exception {
        final Timeline invalidTimeline = new TimelineBuilder()
                .setDate(LocalDate.now())
                .setMessage(new MarkupString("This is a test"))
                .build();

        presenter.setDateWithTimeline(invalidTimeline.getDate(), invalidTimeline);

        assertFalse(presenter.hasValidTimeline());
        assertFalse(presenter.hasValidTimeline(invalidTimeline.getDate()));
    }
}
