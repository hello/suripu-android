package is.hello.sense.graph.presenters;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;

public class InsightsPresenterTests extends InjectionTests {
    @Inject InsightsPresenter insightsPresenter;

    @Test
    public void update() throws Exception {
        Sync.wrapAfter(insightsPresenter::update, insightsPresenter.insights)
            .assertTrue(insights -> insights.size() == 3);
    }

}
