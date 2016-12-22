package is.hello.sense.interactors.onboarding;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.OnboardingPairSenseInteractor;

public class OnboardingPairSenseInteractorTests extends InjectionTestCase {

    @Inject
    HardwareInteractor hardwareInteractor;

    OnboardingPairSenseInteractor interactor;

    @Before
    public void setUp() {
        interactor = new OnboardingPairSenseInteractor(hardwareInteractor);
    }

    @Test
    public void shouldClearPeripheral() throws Exception {
        assertFalse(interactor.shouldClearPeripheral());
    }

}
