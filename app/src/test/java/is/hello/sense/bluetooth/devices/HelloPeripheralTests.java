package is.hello.sense.bluetooth.devices;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.TestOperationTimeout;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.bluetooth.stacks.TestPeripheralService;
import is.hello.sense.functional.Either;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;
import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class HelloPeripheralTests extends InjectionTests {
    @Inject BluetoothStack stack;

    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -90);
    private TestHelloPeripheral peripheral;

    @Before
    public void initialize() throws Exception {
        peripheralBehavior.reset();

        if (peripheral == null) {
            this.peripheral = new TestHelloPeripheral(new TestPeripheral(stack, peripheralBehavior));
        }
        HelloPeripheral.Tests.setPeripheralService(peripheral, null);
    }


    //region Delegated Methods

    @Test
    public void testIsConnected() throws Exception {
        peripheralBehavior.setServicesResponse(null);
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        peripheral.setPeripheralService(new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY));
        assertTrue(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTED);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTING);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTING);
        assertFalse(peripheral.isConnected());
    }

    @Test
    public void getBondStatus() throws Exception {
        peripheralBehavior.setBondStatus(Peripheral.BOND_NONE);
        assertEquals(Peripheral.BOND_NONE, peripheral.getBondStatus());

        peripheralBehavior.setBondStatus(Peripheral.BOND_BONDING);
        assertEquals(Peripheral.BOND_BONDING, peripheral.getBondStatus());

        peripheralBehavior.setBondStatus(Peripheral.BOND_BONDED);
        assertEquals(Peripheral.BOND_BONDED, peripheral.getBondStatus());
    }

    @Test
    public void getScannedRssi() throws Exception {
        assertEquals(-90, peripheral.getScannedRssi());
    }

    @Test
    public void getAddress() throws Exception {
        assertEquals("ca:15:4f:fa:b7:0b", peripheral.getAddress());
    }

    @Test
    public void getName() throws Exception {
        assertEquals("Sense-Test", peripheral.getName());
    }

    //endregion


    //region Methods w/Logic

    @Test
    public void successfulConnect() throws Exception {
        Either<Peripheral, Throwable> successResponse = Either.left(peripheral.peripheral);
        TestPeripheralService service = new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY);
        Map<UUID, PeripheralService> services = new HashMap<>();
        services.put(service.getUuid(), service);
        peripheralBehavior.setConnectResponse(successResponse)
                        .setCreateBondResponse(successResponse)
                        .setServicesResponse(Either.left(services));

        Sync.last(peripheral.connect());
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
    }

    @Test
    public void failedConnect() throws Exception {
        Either<Peripheral, Throwable> successResponse = Either.left(peripheral.peripheral);

        peripheralBehavior.setCreateBondResponse(successResponse);
        peripheralBehavior.setConnectResponse(Either.right(new BluetoothDisabledError()));
        peripheralBehavior.setServicesResponse(Either.left(Collections.emptyMap()));
        peripheralBehavior.setDisconnectResponse(successResponse);

        Sync.wrap(peripheral.connect())
            .assertThrows(BluetoothDisabledError.class);


        // ---- //


        peripheralBehavior.reset();
        peripheralBehavior.setConnectResponse(successResponse);
        peripheralBehavior.setCreateBondResponse(Either.right(new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_ANDROID_API_CHANGED)));
        peripheralBehavior.setServicesResponse(Either.left(Collections.emptyMap()));
        peripheralBehavior.setDisconnectResponse(successResponse);

        Sync.wrap(peripheral.connect())
            .assertThrows(PeripheralBondAlterationError.class);


        // ---- //


        peripheralBehavior.reset();
        peripheralBehavior.setConnectResponse(successResponse);
        peripheralBehavior.setCreateBondResponse(successResponse);
        peripheralBehavior.setServicesResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.DISCOVER_SERVICES)));
        peripheralBehavior.setDisconnectResponse(successResponse);

        Sync.wrap(peripheral.connect())
            .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void disconnect() throws Exception {
        peripheralBehavior.setDisconnectResponse(Either.left(peripheral.peripheral));

        Sync.wrap(peripheral.disconnect())
            .assertEquals(peripheral);


        peripheralBehavior.reset();
        peripheralBehavior.setDisconnectResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.DISCONNECT)));

        Sync.wrap(peripheral.disconnect())
            .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void subscribe() throws Exception {
        PeripheralService service = new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY);
        HelloPeripheral.Tests.setPeripheralService(peripheral, service);

        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        TestOperationTimeout timeout = TestOperationTimeout.acquire("Subscribe");

        peripheralBehavior.setSubscriptionResponse(Either.left(id));
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);

        Sync.wrap(peripheral.subscribe(id, timeout))
            .assertEquals(id);


        peripheralBehavior.reset();
        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)));
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);

        Sync.wrap(peripheral.subscribe(id, timeout))
            .assertThrows(BluetoothGattError.class);


        peripheralBehavior.reset();
        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)));
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTED);

        Sync.wrap(peripheral.subscribe(id, timeout))
            .assertThrows(PeripheralConnectionError.class);
    }

    @Test
    public void unsubscribe() throws Exception {
        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        TestOperationTimeout timeout = TestOperationTimeout.acquire("Unsubscribe");

        peripheralBehavior.setUnsubscriptionResponse(Either.left(id));

        Sync.wrap(peripheral.unsubscribe(id, timeout))
            .assertEquals(id);


        peripheralBehavior.reset();
        peripheralBehavior.setUnsubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.UNSUBSCRIBE_NOTIFICATION)));

        Sync.wrap(peripheral.unsubscribe(id, timeout))
            .assertThrows(BluetoothGattError.class);
    }

    //endregion


    static class TestHelloPeripheral extends HelloPeripheral<TestHelloPeripheral> {
        TestHelloPeripheral(@NonNull Peripheral peripheral) {
            super(peripheral);
        }

        @Override
        protected UUID getTargetServiceIdentifier() {
            return SenseIdentifiers.SERVICE;
        }

        @Override
        protected UUID getDescriptorIdentifier() {
            return SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG;
        }

        void setPeripheralService(PeripheralService service) {
            this.peripheralService = service;
        }

        Observable<ConnectStatus> connect() {
            return super.connect(TestOperationTimeout.acquire("Discover Services"));
        }
    }
}
