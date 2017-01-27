package is.hello.sense.flows.home.ui.fragments;

import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.FragmentTest;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class AppSettingsFragmentTest extends FragmentTest<AppSettingsFragment> {

    @Test
    public void initializePresenterViewTest() {
        fragment.presenterView = null;
        fragment.initializePresenterView();
        assertNotNull(fragment.presenterView);
    }

    @Test
    public void onCreateTest() {
        fragment.initializePresenterView();
        callOnCreate();
        verify(fragment).addInteractor(eq(fragment.hasVoiceInteractor));
    }

    @Test
    public void onViewCreatedTest() {
        spyOnHasVoiceInteractor();
        callOnViewCreated();
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.hasVoiceInteractor.hasVoice),
                                                  Mockito.anyObject(),
                                                  Mockito.anyObject());
    }

    @Test
    public void onCreateViewTest() {
        fragment.presenterView = spy(fragment.presenterView);
        fragment.hasVoiceInteractor = spy(fragment.hasVoiceInteractor);
        callOnViewCreated();
        verify(fragment).bindAndSubscribe(eq(fragment.hasVoiceInteractor.hasVoice), anyObject(), anyObject());
        verify(fragment.hasVoiceInteractor).update();
    }

    private void spyOnHasVoiceInteractor() {
        fragment.hasVoiceInteractor = Mockito.spy(fragment.hasVoiceInteractor);
    }

}
