package is.hello.sense.bluetooth.devices;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.TestOperationTimeout;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.bluetooth.stacks.TestPeripheralService;
import is.hello.sense.functional.Either;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class HelloPeripheralTests extends InjectionTestCase {
    @Inject BluetoothStack stack;

    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -90);
    private TestHelloPeripheral peripheral;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        peripheralBehavior.reset();

        if (peripheral == null) {
            this.peripheral = new TestHelloPeripheral(new TestPeripheral(stack, peripheralBehavior));
        }
    }


    //region Delegated Methods

    public void testIsConnected() throws Exception {
        peripheralBehavior.setServicesResponse(null);
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setServicesResponse(Either.left(Collections.emptyList()));
        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTED);
        assertTrue(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTED);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_CONNECTING);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(Peripheral.STATUS_DISCONNECTING);
        assertFalse(peripheral.isConnected());
    }

    public void testGetBondStatus() throws Exception {
        peripheralBehavior.setBondStatus(Peripheral.BOND_NONE);
        assertEquals(Peripheral.BOND_NONE, peripheral.getBondStatus());

        peripheralBehavior.setBondStatus(Peripheral.BOND_BONDING);
        assertEquals(Peripheral.BOND_BONDING, peripheral.getBondStatus());

        peripheralBehavior.setBondStatus(Peripheral.BOND_BONDED);
        assertEquals(Peripheral.BOND_BONDED, peripheral.getBondStatus());
    }

    public void testGetScannedRssi() throws Exception {
        assertEquals(-90, peripheral.getScannedRssi());
    }

    public void testGetAddress() throws Exception {
        assertEquals("ca:15:4f:fa:b7:0b", peripheral.getAddress());
    }

    public void testGetName() throws Exception {
        assertEquals("Sense-Test", peripheral.getName());
    }

    //endregion


    //region Methods w/Logic

    public void testSuccessfulConnect() throws Exception {
        Either<Peripheral, Throwable> successResponse = Either.left(peripheral.peripheral);
        TestPeripheralService service = new TestPeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY);
        peripheralBehavior.setConnectResponse(successResponse)
                        .setCreateBondResponse(successResponse)
                        .setServicesResponse(Either.left(Lists.newArrayList(service)));

        Sync.last(peripheral.connect());
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
    }

    public void testFailedConnect() throws Exception {
        Either<Peripheral, Throwable> successResponse = Either.left(peripheral.peripheral);

        peripheralBehavior.setConnectResponse(Either.right(new BluetoothDisabledError()));
        peripheralBehavior.setDisconnectResponse(successResponse);

        Sync.wrap(peripheral.connect())
            .assertThrows(BluetoothDisabledError.class);


        // ---- //


        peripheralBehavior.reset();
        peripheralBehavior.setConnectResponse(successResponse);
        peripheralBehavior.setCreateBondResponse(Either.right(new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_ANDROID_API_CHANGED)));
        peripheralBehavior.setDisconnectResponse(successResponse);

        Sync.wrap(peripheral.connect())
            .assertThrows(PeripheralBondAlterationError.class);

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertFalse(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCONNECT));


        // ---- //


        peripheralBehavior.reset();
        peripheralBehavior.setConnectResponse(successResponse);
        peripheralBehavior.setCreateBondResponse(successResponse);
        peripheralBehavior.setServicesResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.DISCOVER_SERVICES)));
        peripheralBehavior.setDisconnectResponse(successResponse);

        Sync.wrap(peripheral.connect())
            .assertThrows(BluetoothGattError.class);

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCONNECT));
    }

    public void testDisconnect() throws Exception {
        peripheralBehavior.setDisconnectResponse(Either.left(peripheral.peripheral));

        Sync.wrap(peripheral.disconnect())
            .assertEquals(peripheral);


        peripheralBehavior.reset();
        peripheralBehavior.setDisconnectResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.DISCONNECT)));

        Sync.wrap(peripheral.disconnect())
            .assertThrows(BluetoothGattError.class);
    }

    public void testSubscribe() throws Exception {
        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        TestOperationTimeout timeout = TestOperationTimeout.acquire("Subscribe");

        peripheralBehavior.setSubscriptionResponse(Either.left(id));

        Sync.wrap(peripheral.subscribe(id, timeout))
            .assertEquals(id);


        peripheralBehavior.reset();
        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)));

        Sync.wrap(peripheral.subscribe(id, timeout))
            .assertThrows(BluetoothGattError.class);
    }

    public void testUnsubscribe() throws Exception {
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

        Observable<ConnectStatus> connect() {
            return super.connect(TestOperationTimeout.acquire("Discover Services"));
        }
    }
}
