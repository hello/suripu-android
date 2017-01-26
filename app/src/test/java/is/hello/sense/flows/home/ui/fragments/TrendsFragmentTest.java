package is.hello.sense.flows.home.ui.fragments;


import org.junit.Test;
import org.mockito.Mockito;


import is.hello.sense.FragmentTest;
import is.hello.sense.api.model.v2.Trends;

public class TrendsFragmentTest extends FragmentTest<WeekTrendsFragment> {


    @Test
    public void initializePresenterViewTest() {
        fragment.presenterView = null;
        fragment.initializePresenterView();
        Mockito.verify(fragment).isAccountMoreThan2WeeksOld();
        Mockito.verify(fragment).createTrendsAdapter();
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
    public void onUserVisibleTest() {
        fragment.setVisibleToUser(true);
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

    private void spyOnTrendsInteractor() {
        fragment.trendsInteractor = Mockito.spy(fragment.trendsInteractor);
    }

}