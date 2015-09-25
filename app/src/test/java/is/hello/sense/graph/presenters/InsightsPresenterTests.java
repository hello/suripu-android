package is.hello.sense.graph.presenters;

import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InsightsPresenterTests extends InjectionTestCase {
    @Inject InsightsPresenter insightsPresenter;
    @Inject PreferencesPresenter preferences;

    @Test
    public void update() throws Exception {
        final ArrayList<Insight> insights = Sync.wrapAfter(insightsPresenter::update,
                                                           insightsPresenter.insights).last();
        assertThat(insights.size(), is(equalTo(3)));
    }
}
