package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.devices.HelloPeripheral;
import is.hello.buruberi.bluetooth.devices.SenseIdentifiers;
import is.hello.buruberi.bluetooth.devices.SensePeripheral;
import is.hello.buruberi.bluetooth.errors.BluetoothError;
import is.hello.buruberi.bluetooth.errors.OperationTimeoutError;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.Peripheral;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.test.TestPeripheral;
import is.hello.buruberi.bluetooth.stacks.test.TestPeripheralBehavior;
import is.hello.buruberi.bluetooth.stacks.test.TestPeripheralService;
import is.hello.buruberi.util.Either;
import is.hello.sense.api.model.Device;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

import static is.hello.buruberi.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint;
import static is.hello.buruberi.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;
import static is.hello.sense.AssertExtensions.assertNoThrow;
import static is.hello.sense.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HardwarePresenterTests extends InjectionTestCase {
    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private final TestPeripheral testPeripheral;
    private final SensePeripheral peripheral;

    @Inject BluetoothStack stack;
    @Inject HardwarePresenter presenter;

    public HardwarePresenterTests() {
        this.testPeripheral = new TestPeripheral(stack, peripheralBehavior);
        this.peripheral = new SensePeripheral(testPeripheral);
    }

    @Before
    public void initialize() throws Exception {
        HelloPeripheral.Tests.setPeripheralService(peripheral, null);
    }

    @After
    public void tearDown() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, null);
        peripheralBehavior.reset();
    }

    @Test
    public void errorsResetPeripheral() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        OperationTimeoutError error = new OperationTimeoutError(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION);
        peripheralBehavior.setDisconnectResponse(Either.left(testPeripheral));
        peripheralBehavior.setSubscriptionResponse(Either.right(error));
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        HelloPeripheral.Tests.setPeripheralService(peripheral, new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY));

        assertThrows(() -> {
            Sync.last(presenter.currentWifiNetwork());
        });
        assertNull(HardwarePresenter.Tests.getPeripheral(presenter));


        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothError("Test")));

        assertThrows(() -> {
            Sync.last(presenter.currentWifiNetwork());
        });
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));
    }

    @Test
    public void clearsPeripheralOnBluetoothDisable() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        presenter.onBluetoothEnabledChanged(true);
        assertNotNull(HardwarePresenter.Tests.getPeripheral(presenter));

        presenter.onBluetoothEnabledChanged(false);
        assertNull(HardwarePresenter.Tests.getPeripheral(presenter));
    }

    @Test
    public void connectivityGetters() throws Exception {
        TestPeripheralService service = new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY);
        HelloPeripheral.Tests.setPeripheralService(peripheral, service);
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);
        peripheralBehavior.setServicesResponse(Either.left(Collections.emptyMap()));
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);

        assertTrue(presenter.hasPeripheral());
        assertTrue(presenter.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTED);

        assertFalse(presenter.isConnected());

        HardwarePresenter.Tests.setPeripheral(presenter, null);

        assertFalse(presenter.hasPeripheral());
        assertFalse(presenter.isConnected());

        HelloPeripheral.Tests.setPeripheralService(peripheral, null);
    }

    @Test
    public void noDeviceErrors() throws Exception {
        HardwarePresenter.Tests.setPeripheral(presenter, null);

        assertThrowsNoDeviceError(presenter.connectToPeripheral());
        assertThrowsNoDeviceError(presenter.runLedAnimation(SensePeripheral.LedAnimation.STOP));
        assertThrowsNoDeviceError(presenter.scanForWifiNetworks());
        assertThrowsNoDeviceError(presenter.currentWifiNetwork());
        assertThrowsNoDeviceError(presenter.sendWifiCredentials("1234", sec_type.SL_SCAN_SEC_TYPE_OPEN, ""));
        assertThrowsNoDeviceError(presenter.linkAccount());
        assertThrowsNoDeviceError(presenter.linkPill());
        assertThrowsNoDeviceError(presenter.pushData());
        assertThrowsNoDeviceError(presenter.putIntoPairingMode());
        assertThrowsNoDeviceError(presenter.factoryReset(new Device()));
    }

    @Test
    public void wifiSignalStrengthSort() throws Exception {
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

    @Test
    public void clearPeripheral() throws Exception {
        TestPeripheralService service = new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY);
        HelloPeripheral.Tests.setPeripheralService(peripheral, service);
        HardwarePresenter.Tests.setPeripheral(presenter, peripheral);

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        peripheralBehavior.setDisconnectResponse(Either.left(testPeripheral));

        assertTrue(presenter.hasPeripheral());
        assertTrue(presenter.isConnected());

        presenter.clearPeripheral();

        assertFalse(presenter.hasPeripheral());
        assertFalse(presenter.isConnected());

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCONNECT));

        HelloPeripheral.Tests.setPeripheralService(peripheral, null);
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
