package is.hello.sense.interactors;

import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InsightsInteractorTests extends InjectionTestCase {
    @Inject
    InsightsInteractor insightsPresenter;
    @Inject
    PreferencesInteractor preferences;

    @Test
    public void update() throws Exception {
        final ArrayList<Insight> insights = Sync.wrapAfter(insightsPresenter::update,
                                                           insightsPresenter.insights).last();
        assertThat(insights.size(), is(equalTo(3)));
    }
}
