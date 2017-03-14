package is.hello.sense.flows.home.ui.fragments;

import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.FragmentTest;
import is.hello.sense.flows.home.util.FeedViewPagerPresenterDelegate;

import static junit.framework.Assert.assertTrue;

public class FeedPresenterFragmentTest extends FragmentTest<FeedPresenterFragment> {


    @Test
    public void newViewPagerDelegateInstanceTest() {
        assertTrue(fragment.newViewPagerDelegateInstance() instanceof FeedViewPagerPresenterDelegate);
    }

    @Test
    public void onCreateTest() {
        callOnCreate();
        Mockito.verify(fragment).addInteractor(Mockito.eq(fragment.hasVoiceInteractor));
    }

    @Test
    public void onViewCreatedTest() {
        spyOnHasVoiceInteractor();
        callOnViewCreated();
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.hasVoiceInteractor.hasVoice),
                                                  Mockito.any(),
                                                  Mockito.any());
        Mockito.verify(fragment.hasVoiceInteractor).update();
    }

    @Test
    public void bindVoiceSettingsTest() {
        spyOnPresenterView();
        fragment.bindVoiceSettings(true);
        Mockito.verify(fragment.presenterView).unlockViewPager(Mockito.eq(fragment));
        fragment.bindVoiceSettings(false);
        Mockito.verify(fragment.presenterView).lockViewPager(Mockito.eq(fragment.getStartingItemPosition()));
    }


    private void spyOnHasVoiceInteractor() {
        fragment.hasVoiceInteractor = Mockito.spy(fragment.hasVoiceInteractor);
    }
}
