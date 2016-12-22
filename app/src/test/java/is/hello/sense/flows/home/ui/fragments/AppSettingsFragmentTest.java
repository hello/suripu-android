package is.hello.sense.flows.home.ui.fragments;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.graph.SenseTestCase;

import static org.robolectric.util.FragmentTestUtil.startFragment;

public class AppSettingsFragmentTest extends SenseTestCase {
    AppSettingsFragment fragment;


    @Before
    public void setUp() throws Exception {
        fragment = new AppSettingsFragment();
        startFragment(fragment);
        fragment = Mockito.spy(fragment);
    }

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
