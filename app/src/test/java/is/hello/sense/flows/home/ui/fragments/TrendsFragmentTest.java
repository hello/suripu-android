package is.hello.sense.flows.home.ui.fragments;


import org.junit.Test;
import org.mockito.Mockito;


import is.hello.sense.FragmentTest;
import is.hello.sense.api.model.v2.Trends;


public class TrendsFragmentTest extends FragmentTest<WeekTrendsFragment> {


    @Test
    public void initializePresenterViewTest() {
        spyOnPresenterChildDelegate();
        fragment.presenterView = null;
        fragment.initializePresenterView();
        Mockito.verify(fragment).isAccountMoreThan2WeeksOld();
        Mockito.verify(fragment).createTrendsAdapter();
        Mockito.verify(fragment.presenterChildDelegate).onViewInitialized();
    }

    @Test
    public void onCreateTest() {
        spyOnTrendsInteractor();
        callOnCreate();
        Mockito.verify(fragment).addInteractor(Mockito.eq(fragment.trendsInteractor));
    }

    @Test
    public void onViewCreatedTest() {
        spyOnTrendsInteractor();
        callOnViewCreated();
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.trendsInteractor.trends), Mockito.anyObject(), Mockito.anyObject());
    }

    @Test
    public void setUserVisibleHintTest() {
        spyOnPresenterChildDelegate();
        fragment.setUserVisibleHint(true);
        Mockito.verify(fragment.presenterChildDelegate).setUserVisibleHint(Mockito.eq(true));
    }

    @Test
    public void onResumeTest() {
        spyOnPresenterChildDelegate();
        callOnResume();
        Mockito.verify(fragment.presenterChildDelegate).onResume();
    }

    @Test
    public void onPauseTest() {
        spyOnPresenterChildDelegate();
        fragment.onPause();
        Mockito.verify(fragment.presenterChildDelegate).onPause();
    }

    @Test
    public void onUserVisibleTest() {
        fragment.onUserVisible();
        Mockito.verify(fragment).fetchTrends();
    }

    @Test
    public void fetchTrendsTest() {
        spyOnTrendsInteractor();
        fragment.fetchTrends();
        Mockito.verify(fragment.trendsInteractor).setTimeScale(Mockito.eq(Trends.TimeScale.LAST_WEEK));
        Mockito.verify(fragment.trendsInteractor).update();
    }

    @Test
    public void scrollUpTest() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.scrollUp();
        Mockito.verify(fragment.presenterView).scrollUp();
    }

    @Test
    public void bindTrendsTest() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        final Trends trends = Mockito.mock(Trends.class);
        fragment.bindTrends(trends);
        Mockito.verify(fragment.presenterView).updateTrends(Mockito.eq(trends));
    }

    @Test
    public void presentErrorTest() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.presentError(new Throwable());
        Mockito.verify(fragment.presenterView).showError();
    }

    @Test
    public void isFinishedTest() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.isFinished();
        Mockito.verify(fragment.presenterView).refreshRecyclerView();
    }

    private void spyOnTrendsInteractor() {
        fragment.trendsInteractor = Mockito.spy(fragment.trendsInteractor);
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