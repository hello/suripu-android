package is.hello.sense.interactors;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.BatteryUtil;
import is.hello.sense.util.Sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

public class PhoneBatteryInteractorTests extends InjectionTestCase{
    @Inject
    PhoneBatteryInteractor phoneBatteryPresenter;

    final static BatteryUtil.Operation WILL_FAIL_OPERATION = new BatteryUtil.Operation(1.0, true);
    final static BatteryUtil.Operation WILL_PASS_OPERATION = new BatteryUtil.Operation(0, false);


    public PhoneBatteryInteractorTests(){
        super();
        doReturn(true).when(phoneBatteryPresenter.batteryUtil)
                .canPerformOperation(WILL_PASS_OPERATION);

        doReturn(false).when(phoneBatteryPresenter.batteryUtil)
                      .canPerformOperation(WILL_FAIL_OPERATION);
    }

    @Before
    public void setUp(){
        phoneBatteryPresenter.withAnyOperation(null); //this clears operation list
    }

    @Test
    public void withNoOperations() {
        final boolean hasEnoughBattery = Sync.wrapAfter(phoneBatteryPresenter::update,
                                                        phoneBatteryPresenter.enoughBattery)
                                             .last();

        assertFalse(hasEnoughBattery);
    }

    @Test
    public void withHasEnoughOperation() {
        phoneBatteryPresenter.withAnyOperation(Collections.singletonList(WILL_PASS_OPERATION));

        final boolean hasEnoughBattery = Sync.wrapAfter(phoneBatteryPresenter::update,
                                                        phoneBatteryPresenter.enoughBattery)
                                             .last();

        assertTrue(hasEnoughBattery);
    }

    @Test
    public void withNotEnoughOperation() {
        phoneBatteryPresenter.withAnyOperation(Collections.singletonList(WILL_FAIL_OPERATION));
        final boolean hasEnoughBattery = Sync.wrapAfter(phoneBatteryPresenter::update,
                                                        phoneBatteryPresenter.enoughBattery)
                                             .last();

        assertFalse(hasEnoughBattery);
    }

    @Test
    public void withHasEnoughForAtLeastOneOperation() {
        phoneBatteryPresenter.withAnyOperation(
                Arrays.asList(WILL_FAIL_OPERATION,
                              WILL_PASS_OPERATION));

        final boolean hasEnoughBattery = Sync.wrapAfter(phoneBatteryPresenter::update,
                                                        phoneBatteryPresenter.enoughBattery)
                                             .last();
        assertTrue(hasEnoughBattery);
    }

    @Test
    public void withHasEnoughForNoOperation() {
        phoneBatteryPresenter.withAnyOperation(
                Arrays.asList(WILL_FAIL_OPERATION,
                              WILL_FAIL_OPERATION));

        final boolean hasEnoughBattery = Sync.wrapAfter(phoneBatteryPresenter::update,
                                                        phoneBatteryPresenter.enoughBattery)
                                             .last();
        assertFalse(hasEnoughBattery);
    }
}
