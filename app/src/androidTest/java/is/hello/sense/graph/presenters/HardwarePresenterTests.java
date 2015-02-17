package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.functional.Either;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.ModelHelper;
import is.hello.sense.util.Sync;

import static is.hello.sense.AssertExtensions.assertThrows;

public class HardwarePresenterTests extends InjectionTestCase {
    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private SensePeripheral peripheral;

    @Inject BluetoothStack stack;
    @Inject HardwarePresenter hardwarePresenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (peripheral == null) {
            this.peripheral = new SensePeripheral(new TestPeripheral(stack, peripheralBehavior));
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        hardwarePresenter.setPeripheral(null);
        peripheralBehavior.reset();
    }

    public void testErrorsResetPeripheral() throws Exception {
        hardwarePresenter.setPeripheral(peripheral);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(hardwarePresenter));

        OperationTimeoutError error = new OperationTimeoutError(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION);
        peripheralBehavior.setSubscriptionResponse(Either.right(error));

        assertThrows(() -> Sync.last(hardwarePresenter.currentWifiNetwork()));
        assertNull(HardwarePresenter.Tests.getPeripheral(hardwarePresenter));
    }

    public void testClearsPeripheralOnBluetoothDisable() throws Exception {
        fail();
    }

    public void testConnectivityGetters() throws Exception {
        fail();
    }

    public void testNoDeviceErrors() throws Exception {
        fail();
    }

    public void testWifiNetworksSorted() throws Exception {
        fail();
    }

    public void testClearPeripheral() throws Exception {
        fail();
    }
}
