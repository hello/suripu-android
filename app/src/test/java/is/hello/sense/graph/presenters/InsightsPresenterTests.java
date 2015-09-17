package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;

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

    @SuppressLint("CommitPrefEdits")
    @Test
    public void updateClearsUnreadStatus() throws Exception {
        preferences.edit()
                   .putBoolean(PreferencesPresenter.HAS_UNREAD_INSIGHT_ITEMS, true)
                   .commit();

        final ArrayList<Insight> insights = Sync.wrapAfter(insightsPresenter::update,
                                                           insightsPresenter.insights).last();
        assertThat(insights.size(), is(equalTo(3)));

        // The `doOnCompleted` handler requires a little bit of a wait to fire.
        Thread.sleep(2);

        assertThat(preferences.getBoolean(PreferencesPresenter.HAS_UNREAD_INSIGHT_ITEMS, false),
                   is(false));
    }

}
