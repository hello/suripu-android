package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.functional.Either;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static is.hello.sense.AssertExtensions.assertNoThrow;
import static is.hello.sense.AssertExtensions.assertThrows;

public class HardwarePresenterTests extends InjectionTestCase {
    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private SensePeripheral peripheral;

    @Inject BluetoothStack stack;
    @Inject HardwarePresenter presenter;

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

        HardwarePresenter.Tests.setPeripheral(presenter, null);
        peripheralBehavior.reset();
    }

    public void testErrorsResetPeripheral() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        OperationTimeoutError error = new OperationTimeoutError(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION);
        peripheralBehavior.setSubscriptionResponse(Either.right(error));

        assertThrows(() -> Sync.last(presenter.currentWifiNetwork()));
        assertNull(HardwarePresenter.Tests.getPeripheral(presenter));


        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothError()));

        assertThrows(() -> Sync.last(presenter.currentWifiNetwork()));
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));
    }

    public void testClearsPeripheralOnBluetoothDisable() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        presenter.onBluetoothEnabledChanged(true);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        presenter.onBluetoothEnabledChanged(false);
        assertNull(HardwarePresenter.Tests.getPeripheral(presenter));
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
