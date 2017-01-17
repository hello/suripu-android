package is.hello.sense.flows.home.ui.fragments;

import org.junit.Test;

import is.hello.sense.FragmentTest;

import static org.mockito.Mockito.*;

public class AppSettingsFragmentTest extends FragmentTest<AppSettingsFragment> {


    @Test
    public void onCreateTest() {
        fragment.initializePresenterView();
        callOnCreate();
        verify(fragment).addInteractor(eq(fragment.hasVoiceInteractor));
    }


    @Test
    public void onCreateViewTest() {
        fragment.presenterView = spy(fragment.presenterView);
        fragment.hasVoiceInteractor = spy(fragment.hasVoiceInteractor);
        callOnViewCreated();
        verify(fragment).bindAndSubscribe(eq(fragment.hasVoiceInteractor.hasVoice), anyObject(), anyObject());
        verify(fragment.hasVoiceInteractor).update();
    }
}
