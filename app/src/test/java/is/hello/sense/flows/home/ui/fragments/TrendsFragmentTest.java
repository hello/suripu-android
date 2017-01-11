package is.hello.sense.flows.home.ui.fragments;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;


import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.graph.SenseTestCase;

import static org.robolectric.util.FragmentTestUtil.startFragment;

public class TrendsFragmentTest extends SenseTestCase {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    private TrendsFragment fragment;

    @Before
    public void setUp() throws Exception {
        fragment = new WeekTrendsFragment();
        startFragment(fragment);
        fragment = Mockito.spy(fragment);
    }

    @Test
    public void initializePresenterViewCallsCorrectMethods() {
        spyOnPresenterChildDelegate();
        fragment.presenterView = null;
        fragment.initializePresenterView();
        Mockito.verify(fragment).isAccountMoreThan2WeeksOld();
        Mockito.verify(fragment).createTrendsAdapter();
        Mockito.verify(fragment.presenterChildDelegate).onViewInitialized();
    }

    @Test
    public void onCreateCallsCorrectMethods() {
        fragment.trendsInteractor = Mockito.spy(fragment.trendsInteractor);
        fragment.onCreate(null);
        Mockito.verify(fragment).addInteractor(Mockito.eq(fragment.trendsInteractor));
    }

    @Test
    public void onViewCreatedCallsCorrectMethods() {
        fragment.trendsInteractor = Mockito.spy(fragment.trendsInteractor);
        fragment.onViewCreated(fragment.getView(), null);
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.trendsInteractor.trends), Mockito.anyObject(), Mockito.anyObject());
    }

    @Test
    public void setUserVisibleHintCallsCorrectMethods() {
        spyOnPresenterChildDelegate();
        fragment.setUserVisibleHint(true);
        Mockito.verify(fragment.presenterChildDelegate).setUserVisibleHint(Mockito.eq(true));
    }

    @Test
    public void onResumeCallsCorrectMethods() {
        spyOnPresenterChildDelegate();
        fragment.onResume();
        Mockito.verify(fragment.presenterChildDelegate).onResume();
    }

    @Test
    public void onPauseCallsCorrectMethods() {
        spyOnPresenterChildDelegate();
        fragment.onPause();
        Mockito.verify(fragment.presenterChildDelegate).onPause();
    }

    @Test
    public void onUserVisibleCallsCorrectMethods() {
        fragment.onUserVisible();
        Mockito.verify(fragment).fetchTrends();
    }

    @Test
    public void fetchTrendsCallsCorrectMethods() {
        fragment.trendsInteractor = Mockito.spy(fragment.trendsInteractor);
        fragment.fetchTrends();
        Mockito.verify(fragment.trendsInteractor).setTimeScale(Mockito.eq(Trends.TimeScale.LAST_WEEK));
        Mockito.verify(fragment.trendsInteractor).update();
    }

    @Test
    public void scrollUpCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.scrollUp();
        Mockito.verify(fragment.presenterView).scrollUp();
    }

    @Test
    public void bindTrendsCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        final Trends trends = Mockito.mock(Trends.class);
        fragment.bindTrends(trends);
        Mockito.verify(fragment.presenterView).updateTrends(Mockito.eq(trends));
    }

    @Test
    public void presentErrorCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.presentError(new Throwable());
        Mockito.verify(fragment.presenterView).showError();
    }

    @Test
    public void isFinishedCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.isFinished();
        Mockito.verify(fragment.presenterView).refreshRecyclerView();
    }


    private void spyOnPresenterChildDelegate() {
        try {
            changeFinalFieldValue(fragment,
                                  fragment.getClass().getField("presenterChildDelegate"),
                                  Mockito.spy(fragment.presenterChildDelegate));
        } catch (final Exception ignored) {

        }
    }
}