package is.hello.sense.flows.settings.ui.fragments;

import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.FragmentTest;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class AppSettingsFragmentTest extends FragmentTest<AppSettingsFragment> {

    @Test
    public void initializePresenterViewTest() {
        fragment.senseView = null;
        fragment.initializeSenseView();
        assertNotNull(fragment.senseView);
    }

    @Test
    public void onCreateTest() {
        fragment.initializeSenseView();
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
        fragment.senseView = Mockito.spy(fragment.senseView);
        fragment.hasVoiceInteractor = Mockito.spy(fragment.hasVoiceInteractor);
        callOnViewCreated();
        verify(fragment).bindAndSubscribe(eq(fragment.hasVoiceInteractor.hasVoice), anyObject(), anyObject());
        verify(fragment.hasVoiceInteractor).update();
    }

    private void spyOnHasVoiceInteractor() {
        fragment.hasVoiceInteractor = Mockito.spy(fragment.hasVoiceInteractor);
    }

}
