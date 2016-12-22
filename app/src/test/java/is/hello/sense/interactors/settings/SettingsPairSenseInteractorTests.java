package is.hello.sense.interactors.settings;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.SettingsPairSenseInteractor;

import static org.junit.Assert.assertTrue;

public class SettingsPairSenseInteractorTests extends InjectionTestCase {

    @Inject
    HardwareInteractor hardwareInteractor;

    SettingsPairSenseInteractor interactor;

    @Before
    public void setUp() {
        interactor = new SettingsPairSenseInteractor(hardwareInteractor);
    }

    @Test
    public void shouldClearPeripheral() throws Exception {
        assertTrue(interactor.shouldClearPeripheral());
    }

}
