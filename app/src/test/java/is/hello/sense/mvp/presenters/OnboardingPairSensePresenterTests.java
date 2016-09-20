package is.hello.sense.mvp.presenters;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.presenters.OnboardingPairSensePresenter;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.ui.common.UserSupport;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class OnboardingPairSensePresenterTests extends InjectionTestCase{
    @Inject
    OnboardingPairSensePresenter onboardingPairSensePresenter;

    private static PairSensePresenter.Output outputView;
    private static UserSupport.HelpStep helpStep;

    @BeforeClass
    public static void setUpMocks(){
        outputView = Mockito.mock(PairSensePresenter.Output.class);
        helpStep = UserSupport.HelpStep.PAIRING_SENSE_BLE;
        when(outputView.isResumed()).thenReturn(true);
        when(outputView.canObservableEmit()).thenReturn(true);
    }

    @Before
    public void setUpView(){
        onboardingPairSensePresenter.setView(outputView);
    }

    @After
    public void removeView(){
        onboardingPairSensePresenter.onDestroyView();
        onboardingPairSensePresenter.onDetach();
        onboardingPairSensePresenter.removeView(outputView);
    }

    @Test
    public void showToolbarHelp(){
        onboardingPairSensePresenter.showToolbarHelp();
        Mockito.verify(outputView).showHelpUri(helpStep);
    }

    @Test
    public void shouldUseDefaultRunnableBackPressBehavior(){
        final Runnable doNothing = () -> {};
        onboardingPairSensePresenter.onBackPressed(doNothing);
        Mockito.verify(outputView, times(0)).cancelFlow();
    }

}
