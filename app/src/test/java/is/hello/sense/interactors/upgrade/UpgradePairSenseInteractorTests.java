package is.hello.sense.interactors.upgrade;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.SwapSenseInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.UpgradePairSenseInteractor;
import is.hello.sense.interactors.CurrentSenseInteractor;

import static org.junit.Assert.assertFalse;

public class UpgradePairSenseInteractorTests extends InjectionTestCase {

    @Inject
    HardwareInteractor hardwareInteractor;

    @Inject
    SwapSenseInteractor swapSenseInteractor;

    @Inject
    DevicesInteractor devicesInteractor;

    CurrentSenseInteractor currentSenseInteractor;
    UpgradePairSenseInteractor interactor;

    @Before
    public void setU() {
        currentSenseInteractor = new CurrentSenseInteractor(devicesInteractor);
        interactor = new UpgradePairSenseInteractor(hardwareInteractor, swapSenseInteractor, currentSenseInteractor);
    }

    @Test
    public void shouldClearPeripheral() throws Exception {
        assertFalse(interactor.shouldClearPeripheral());
    }
}
