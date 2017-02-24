package is.hello.sense.flows.home.ui.fragments;


import org.junit.Test;
import org.mockito.Mockito;


import is.hello.sense.FragmentTest;
import is.hello.sense.api.model.v2.Trends;

public class TrendsFragmentTest extends FragmentTest<WeekTrendsFragment> {


    @Test
    public void initializePresenterViewTest() {
        fragment.senseView = null;
        fragment.initializeSenseView();
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
        fragment.senseView = Mockito.spy(fragment.senseView);
        fragment.scrollUp();
        Mockito.verify(fragment.senseView).scrollUp();
    }

    @Test
    public void bindTrendsTest() {
        fragment.senseView = Mockito.spy(fragment.senseView);
        final Trends trends = Mockito.mock(Trends.class);
        fragment.bindTrends(trends);
        Mockito.verify(fragment.senseView).updateTrends(Mockito.eq(trends));
    }

    @Test
    public void presentErrorTest() {
        fragment.senseView = Mockito.spy(fragment.senseView);
        fragment.presentError(new Throwable());
        Mockito.verify(fragment.senseView).showError();
    }

    private void spyOnTrendsInteractor() {
        fragment.trendsInteractor = Mockito.spy(fragment.trendsInteractor);
    }

}