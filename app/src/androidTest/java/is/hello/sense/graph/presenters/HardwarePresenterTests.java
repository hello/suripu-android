package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.functional.Either;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

import static is.hello.sense.AssertExtensions.assertNoThrow;
import static is.hello.sense.AssertExtensions.assertThrows;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;

public class HardwarePresenterTests extends InjectionTestCase {
    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private TestPeripheral testPeripheral;
    private SensePeripheral peripheral;

    @Inject BluetoothStack stack;
    @Inject HardwarePresenter presenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (testPeripheral == null) {
            this.testPeripheral = new TestPeripheral(stack, peripheralBehavior);
            this.peripheral = new SensePeripheral(testPeripheral);
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

        assertThrows(() -> {
            Sync.last(presenter.currentWifiNetwork());
        });
        assertNull(HardwarePresenter.Tests.getPeripheral(presenter));


        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothError()));

        assertThrows(() -> {
            Sync.last(presenter.currentWifiNetwork());
        });
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
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);

        assertTrue(presenter.hasPeripheral());
        assertTrue(presenter.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTED);

        assertFalse(presenter.isConnected());

        HardwarePresenter.Tests.setPeripheral(presenter, null);

        assertFalse(presenter.hasPeripheral());
        assertFalse(presenter.isConnected());
    }

    public void testNoDeviceErrors() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, null);

        assertThrowsNoDeviceError(presenter.connectToPeripheral());
        assertThrowsNoDeviceError(presenter.runLedAnimation(SensePeripheral.LedAnimation.STOP));
        assertThrowsNoDeviceError(presenter.scanForWifiNetworks());
        assertThrowsNoDeviceError(presenter.currentWifiNetwork());
        assertThrowsNoDeviceError(presenter.sendWifiCredentials("1234", "1234", sec_type.SL_SCAN_SEC_TYPE_OPEN, ""));
        assertThrowsNoDeviceError(presenter.linkAccount());
        assertThrowsNoDeviceError(presenter.linkPill());
        assertThrowsNoDeviceError(presenter.pushData());
        assertThrowsNoDeviceError(presenter.putIntoPairingMode());
        assertThrowsNoDeviceError(presenter.factoryReset());
    }

    public void testWifiSignalStrengthSort() throws Exception {
        // Not currently possible to test the actual wifi networks method
        // due to the relative complexity of mocking multiple responses.
        List<wifi_endpoint> endpoints = Lists.newArrayList(
            wifi_endpoint.newBuilder().setSecurityType(sec_type.SL_SCAN_SEC_TYPE_OPEN).setSsid("Test 1").setRssi(-1000).build(),
            wifi_endpoint.newBuilder().setSecurityType(sec_type.SL_SCAN_SEC_TYPE_OPEN).setSsid("Test 2").setRssi(-50).build(),
            wifi_endpoint.newBuilder().setSecurityType(sec_type.SL_SCAN_SEC_TYPE_OPEN).setSsid("Test 3").setRssi(-4000).build()
        );
        HardwarePresenter.Tests.sortWifiNetworks(presenter, endpoints);
        List<String> endpointNames = Lists.map(endpoints, wifi_endpoint::getSsid);
        assertEquals(Lists.newArrayList("Test 2", "Test 1", "Test 3"), endpointNames);

        assertNoThrow(() -> HardwarePresenter.Tests.sortWifiNetworks(presenter, Lists.newArrayList()));
    }

    public void testClearPeripheral() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        peripheralBehavior.setDisconnectResponse(Either.left(testPeripheral));

        assertTrue(presenter.hasPeripheral());
        assertTrue(presenter.isConnected());

        presenter.clearPeripheral();

        assertFalse(presenter.hasPeripheral());
        assertFalse(presenter.isConnected());

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCONNECT));
    }


    private static <T> void assertThrowsNoDeviceError(@NonNull Observable<T> observable) {
        try {
            Sync.last(observable);
        } catch (HardwarePresenter.NoConnectedPeripheralException ignored) {
            return;
        } catch (Exception e) {
            fail("Unexpected exception '" + e + "' thrown");
        }

        fail("Did not throw no device error");
    }
}
