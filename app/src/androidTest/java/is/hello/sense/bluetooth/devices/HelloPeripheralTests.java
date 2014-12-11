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
import is.hello.sense.bluetooth.stacks.TestOperationTimeout;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.functional.Either;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;
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
        peripheralBehavior.setConnectResponse(successResponse)
                        .setCreateBondResponse(successResponse)
                        .setServicesResponse(Either.left(Collections.emptyList()));

        SyncObserver<HelloPeripheral.ConnectStatus> connect = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.connect());
        connect.await();

        assertNull(connect.getError());
        assertEquals(4, connect.getResults().size());

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
    }

    public void testFailedConnect() throws Exception {
        Either<Peripheral, Throwable> successResponse = Either.left(peripheral.peripheral);

        peripheralBehavior.setConnectResponse(Either.right(new BluetoothDisabledError()));
        peripheralBehavior.setDisconnectResponse(successResponse);

        SyncObserver<HelloPeripheral.ConnectStatus> connect1 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.connect());
        connect1.await();

        assertNotNull(connect1.getError());
        assertTrue(connect1.getError() instanceof BluetoothDisabledError);
        assertEquals(1, connect1.getResults().size());

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertFalse(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertFalse(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));


        // ---- //


        peripheralBehavior.reset();
        peripheralBehavior.setConnectResponse(successResponse);
        peripheralBehavior.setCreateBondResponse(Either.right(new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_ANDROID_API_CHANGED)));
        peripheralBehavior.setDisconnectResponse(successResponse);

        SyncObserver<HelloPeripheral.ConnectStatus> connect2 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.connect());
        connect2.await();

        assertNotNull(connect2.getError());
        assertTrue(connect2.getError() instanceof PeripheralBondAlterationError);
        assertEquals(2, connect2.getResults().size());

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertFalse(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
        assertFalse(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCONNECT));


        // ---- //


        peripheralBehavior.reset();
        peripheralBehavior.setConnectResponse(successResponse);
        peripheralBehavior.setCreateBondResponse(successResponse);
        peripheralBehavior.setServicesResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE)));
        peripheralBehavior.setDisconnectResponse(successResponse);

        SyncObserver<HelloPeripheral.ConnectStatus> connect3 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.connect());
        connect3.await();

        assertNotNull(connect3.getError());
        assertTrue(connect3.getError() instanceof BluetoothGattError);
        assertEquals(3, connect3.getResults().size());

        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.CREATE_BOND));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCOVER_SERVICES));
        assertFalse(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.DISCONNECT));
    }

    public void testDisconnect() throws Exception {
        peripheralBehavior.setDisconnectResponse(Either.left(peripheral.peripheral));

        SyncObserver<TestHelloPeripheral> disconnect1 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.disconnect());
        disconnect1.await();

        assertNull(disconnect1.getError());
        assertEquals(peripheral, disconnect1.getSingle());


        peripheralBehavior.reset();
        peripheralBehavior.setDisconnectResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE)));

        SyncObserver<TestHelloPeripheral> disconnect2 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.disconnect());
        disconnect2.await();

        assertNotNull(disconnect2.getError());
        assertTrue(disconnect2.getError() instanceof BluetoothGattError);
        assertEquals(0, disconnect2.getResults().size());
    }

    public void testSubscribe() throws Exception {
        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        TestOperationTimeout timeout = TestOperationTimeout.acquire("Subscribe");

        peripheralBehavior.setSubscriptionResponse(Either.left(id));

        SyncObserver<UUID> subscribe1 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.subscribe(id, timeout));
        subscribe1.await();

        assertNull(subscribe1.getError());
        assertEquals(id, subscribe1.getSingle());
        assertTrue(timeout.wasRecycled());


        peripheralBehavior.reset();
        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION)));

        SyncObserver<UUID> subscribe2 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.subscribe(id, timeout));
        subscribe2.await();

        assertNotNull(subscribe2.getError());
        assertTrue(subscribe2.getError() instanceof BluetoothGattError);
        assertTrue(timeout.wasRecycled());
    }

    public void testUnsubscribe() throws Exception {
        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        TestOperationTimeout timeout = TestOperationTimeout.acquire("Unsubscribe");

        peripheralBehavior.setUnsubscriptionResponse(Either.left(id));

        SyncObserver<UUID> subscribe1 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.unsubscribe(id, timeout));
        subscribe1.await();

        assertNull(subscribe1.getError());
        assertEquals(id, subscribe1.getSingle());
        assertTrue(timeout.wasRecycled());


        peripheralBehavior.reset();
        peripheralBehavior.setUnsubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION)));

        SyncObserver<UUID> subscribe2 = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, peripheral.unsubscribe(id, timeout));
        subscribe2.await();

        assertNotNull(subscribe2.getError());
        assertTrue(subscribe2.getError() instanceof BluetoothGattError);
        assertTrue(timeout.wasRecycled());
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
