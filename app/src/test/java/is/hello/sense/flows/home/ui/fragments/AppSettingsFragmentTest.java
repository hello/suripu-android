package is.hello.sense.flows.home.ui.fragments;

import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.FragmentTest;

public class AppSettingsFragmentTest extends FragmentTest<AppSettingsFragment> {


    @Test
    public void onCreateCallsCorrectMethods() {
        fragment.initializePresenterView();
        fragment.onCreate(null);
        Mockito.verify(fragment).addInteractor(Mockito.eq(fragment.hasVoiceInteractor));
    }


    @Test
    public void onCreateViewCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.hasVoiceInteractor = Mockito.spy(fragment.hasVoiceInteractor);
        fragment.onViewCreated(fragment.getView(), null);
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.hasVoiceInteractor.hasVoice), Mockito.anyObject(), Mockito.anyObject());
        Mockito.verify(fragment.hasVoiceInteractor).update();
    }
}
