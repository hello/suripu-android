package is.hello.sense.flows.home.interactors;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineBuilder;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class LastNightInteractorTest extends InjectionTestCase {
    @Inject
    LastNightInteractor lastNightInteractor;

    @Test
    public void getValidTimeline() throws Exception {
        final Timeline validTimeline = new TimelineBuilder().setScore(1, ScoreCondition.IDEAL).build();
        final Timeline result = lastNightInteractor.getValidTimeline(validTimeline);

        assertThat(result, equalTo(validTimeline));
    }

    @Test
    public void update() throws Exception {
        final LastNightInteractor lastNightInteractorSpy = spy(lastNightInteractor);
        final Timeline result = Sync.wrapAfter(lastNightInteractorSpy::update,
                                               lastNightInteractorSpy.timeline)
                                    .last();
        verify(lastNightInteractorSpy).getValidTimeline(any(Timeline.class));
    }

}