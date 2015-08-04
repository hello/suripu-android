package is.hello.sense.bluetooth.sense;

import android.bluetooth.BluetoothGatt;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.BluetoothDisabledError;
import is.hello.buruberi.bluetooth.errors.BluetoothGattError;
import is.hello.buruberi.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.buruberi.bluetooth.errors.PeripheralConnectionError;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.test.FakeBluetoothStack;
import is.hello.buruberi.bluetooth.stacks.test.FakeBluetoothStackBehavior;
import is.hello.buruberi.bluetooth.stacks.test.FakeGattPeripheral;
import is.hello.buruberi.bluetooth.stacks.test.FakeOperationTimeout;
import is.hello.buruberi.bluetooth.stacks.test.FakePeripheralBehavior;
import is.hello.buruberi.bluetooth.stacks.test.FakePeripheralService;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.AdvertisingDataBuilder;
import is.hello.buruberi.util.Either;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.Sync;
import rx.functions.Func1;

import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SensePeripheralTests extends SenseTestCase {
    private static final String TEST_DEVICE_ID = "CA154FFA";

    private final FakeBluetoothStackBehavior stackBehavior = new FakeBluetoothStackBehavior();
    private final BluetoothStack stack = new FakeBluetoothStack(stackBehavior);

    private final FakePeripheralBehavior peripheralBehavior = new FakePeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private SensePeripheral peripheral;

    @Before
    public void initialize() throws Exception {
        stackBehavior.reset();

        if (peripheral == null) {
            this.peripheral = new SensePeripheral(new FakeGattPeripheral(stack, peripheralBehavior));
        }
    }


    //region Discovery

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discovery() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        AdvertisingData advertisingData = builder.build();

        FakePeripheralBehavior device1 = new FakePeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device1.setAdvertisingData(advertisingData);
        stackBehavior.addPeripheralInRange(device1);

        FakePeripheralBehavior device2 = new FakePeripheralBehavior("Sense-Test2", "c2:18:4e:fb:b3:0a", -90);
        device2.setAdvertisingData(advertisingData);
        stackBehavior.addPeripheralInRange(device2);

        PeripheralCriteria peripheralCriteria = new PeripheralCriteria();
        Sync.wrap(SensePeripheral.discover(stack, peripheralCriteria))
            .assertTrue(new Func1<List<SensePeripheral>, Boolean>() {
                @Override
                public Boolean call(List<SensePeripheral> peripherals) {
                    return peripherals.size() == 2;
                }
            });
    }

    @Test
    public void rediscovery() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        AdvertisingData advertisingData = builder.build();

        FakePeripheralBehavior device = new FakePeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device.setAdvertisingData(advertisingData);
        stackBehavior.addPeripheralInRange(device);

        Sync.wrap(SensePeripheral.rediscover(stack, TEST_DEVICE_ID, false))
            .assertTrue(new Func1<SensePeripheral, Boolean>() {
                @Override
                public Boolean call(SensePeripheral peripheral) {
                    return "Sense-Test".equals(peripheral.getName());
                }
            });
    }

    //endregion


    //region Delegated Methods

    @Test
    public void testIsConnected() throws Exception {
        peripheralBehavior.setServicesResponse(null);
        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_CONNECTED);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_CONNECTED);
        SensePeripheral.Testing.setPeripheralService(peripheral, new FakePeripheralService(SenseIdentifiers.SERVICE,
                PeripheralService.SERVICE_TYPE_PRIMARY));
        assertTrue(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_DISCONNECTED);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_CONNECTING);
        assertFalse(peripheral.isConnected());

        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_DISCONNECTING);
        assertFalse(peripheral.isConnected());
    }

    @Test
    public void getBondStatus() throws Exception {
        peripheralBehavior.setBondStatus(GattPeripheral.BOND_NONE);
        assertEquals(GattPeripheral.BOND_NONE, peripheral.getBondStatus());

        peripheralBehavior.setBondStatus(GattPeripheral.BOND_BONDING);
        assertEquals(GattPeripheral.BOND_BONDING, peripheral.getBondStatus());

        peripheralBehavior.setBondStatus(GattPeripheral.BOND_BONDED);
        assertEquals(GattPeripheral.BOND_BONDED, peripheral.getBondStatus());
    }

    @Test
    public void getScannedRssi() throws Exception {
        assertEquals(-50, peripheral.getScannedRssi());
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
        Either<GattPeripheral, Throwable> successResponse = Either.left(SensePeripheral.Testing.getPeripheral(peripheral));
        FakePeripheralService service = new FakePeripheralService(SenseIdentifiers.SERVICE, PeripheralService.SERVICE_TYPE_PRIMARY);
        Map<UUID, PeripheralService> services = new HashMap<>();
        services.put(service.getUuid(), service);
        peripheralBehavior.setConnectResponse(successResponse)
                .setCreateBondResponse(successResponse)
                .setServicesResponse(Either.left(services));

        Sync.last(peripheral.connect());
        assertTrue(peripheralBehavior.wasMethodCalled(FakePeripheralBehavior.Method.CONNECT));
        assertTrue(peripheralBehavior.wasMethodCalled(FakePeripheralBehavior.Method.CREATE_BOND));
        assertTrue(peripheralBehavior.wasMethodCalled(FakePeripheralBehavior.Method.DISCOVER_SERVICES));
    }

    @Test
    public void failedConnect() throws Exception {
        Either<GattPeripheral, Throwable> successResponse = Either.left(SensePeripheral.Testing.getPeripheral(peripheral));

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
        peripheralBehavior.setDisconnectResponse(Either.left(SensePeripheral.Testing.getPeripheral(peripheral)));

        Sync.wrap(peripheral.disconnect())
                .assertEquals(peripheral);


        peripheralBehavior.reset();
        peripheralBehavior.setDisconnectResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.DISCONNECT)));

        Sync.wrap(peripheral.disconnect())
                .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void subscribeResponse() throws Exception {
        SensePeripheral.Testing.setPeripheralService(peripheral, new FakePeripheralService(SenseIdentifiers.SERVICE,
                PeripheralService.SERVICE_TYPE_PRIMARY));

        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        FakeOperationTimeout timeout = FakeOperationTimeout.acquire("Subscribe");

        peripheralBehavior.setSubscriptionResponse(Either.left(id));
        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_CONNECTED);

        Sync.wrap(peripheral.subscribeResponse(timeout))
                .assertEquals(id);


        peripheralBehavior.reset();
        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)));
        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_CONNECTED);

        Sync.wrap(peripheral.subscribeResponse(timeout))
                .assertThrows(BluetoothGattError.class);


        peripheralBehavior.reset();
        peripheralBehavior.setSubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)));
        peripheralBehavior.setConnectionStatus(GattPeripheral.STATUS_DISCONNECTED);

        Sync.wrap(peripheral.subscribeResponse(timeout))
                .assertThrows(PeripheralConnectionError.class);
    }

    @Test
    public void unsubscribeResponse() throws Exception {
        UUID id = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        FakeOperationTimeout timeout = FakeOperationTimeout.acquire("Unsubscribe");

        peripheralBehavior.setUnsubscriptionResponse(Either.left(id));

        Sync.wrap(peripheral.unsubscribeResponse(timeout))
                .assertEquals(id);


        peripheralBehavior.reset();
        peripheralBehavior.setUnsubscriptionResponse(Either.right(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, BluetoothGattError.Operation.UNSUBSCRIBE_NOTIFICATION)));

        Sync.wrap(peripheral.unsubscribeResponse(timeout))
                .assertThrows(BluetoothGattError.class);
    }


    @Test
    public void getDeviceId() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        AdvertisingData advertisingData = builder.build();

        peripheralBehavior.setAdvertisingData(advertisingData);

        assertEquals(TEST_DEVICE_ID, peripheral.getDeviceId());
    }

    @Test
    public void writeLargeCommand() throws Exception {
        //noinspection ConstantConditions
        peripheralBehavior.setWriteCommandResponse(Either.left(null)); // Void could use a singleton instance...

        MorpheusCommand command = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(0)
                .setWifiName("Mostly Radiation")
                .setWifiSSID("00:00:00:00:00:00")
                .setSecurityType(SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .build();
        Sync.last(peripheral.writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND, command.toByteArray()));
        assertTrue(peripheralBehavior.wasMethodCalled(FakePeripheralBehavior.Method.WRITE_COMMAND));
        assertEquals(3, peripheralBehavior.getCalledMethods().size());
    }

    //endregion
}
