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
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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
        final BluetoothStack stack = mock(BluetoothStack.class);

        final AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                    SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        final AdvertisingData advertisingData = builder.build();

        final GattPeripheral device1 = mock(GattPeripheral.class);
        doReturn(stack).when(device1).getStack();
        doReturn("Sense-Test").when(device1).getName();
        doReturn("ca:15:4f:fa:b7:0b").when(device1).getAddress();
        doReturn(-50).when(device1).getScanTimeRssi();
        doReturn(advertisingData).when(device1).getAdvertisingData();

        final GattPeripheral device2 = mock(GattPeripheral.class);
        doReturn(stack).when(device2).getStack();
        doReturn("Sense-Test2").when(device2).getName();
        doReturn("c2:18:4e:fb:b3:0a").when(device2).getAddress();
        doReturn(-90).when(device2).getScanTimeRssi();
        doReturn(advertisingData).when(device2).getAdvertisingData();

        final List<GattPeripheral> peripheralsInRange = Lists.newArrayList(device1, device2);
        doReturn(Observable.just(peripheralsInRange))
                .when(stack)
                .discoverPeripherals(any(PeripheralCriteria.class));

        final PeripheralCriteria peripheralCriteria = new PeripheralCriteria();
        Sync.wrap(SensePeripheral.discover(stack, peripheralCriteria))
            .assertThat(hasSize(2));
    }

    @Test
    public void rediscovery() throws Exception {
        final BluetoothStack stack = mock(BluetoothStack.class);

        final AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        final AdvertisingData advertisingData = builder.build();

        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack).when(device).getStack();
        doReturn("Sense-Test").when(device).getName();
        doReturn("ca:15:4f:fa:b7:0b").when(device).getAddress();
        doReturn(-50).when(device).getScanTimeRssi();
        doReturn(advertisingData).when(device).getAdvertisingData();

        final List<GattPeripheral> peripheralsInRange = Lists.newArrayList(device);
        doReturn(Observable.just(peripheralsInRange))
                .when(stack)
                .discoverPeripherals(any(PeripheralCriteria.class));

        SensePeripheral peripheral = Sync.last(SensePeripheral.rediscover(stack, TEST_DEVICE_ID, false));
        assertThat(peripheral.getName(), is(equalTo("Sense-Test")));
    }

    //endregion


    //region Delegated Methods

    @Test
    public void testIsConnected() throws Exception {
        final BluetoothStack stack = mock(BluetoothStack.class);
        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack).when(device).getStack();
        doReturn(GattPeripheral.STATUS_CONNECTED).when(device).getConnectionStatus();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat(peripheral.isConnected(), is(false));

        SensePeripheral.Testing.setPeripheralService(peripheral, mock(PeripheralService.class));
        assertThat(peripheral.isConnected(), is(true));

        doReturn(GattPeripheral.STATUS_DISCONNECTED).when(device).getConnectionStatus();
        assertThat(peripheral.isConnected(), is(false));

        doReturn(GattPeripheral.STATUS_CONNECTING).when(device).getConnectionStatus();
        assertThat(peripheral.isConnected(), is(false));

        doReturn(GattPeripheral.STATUS_DISCONNECTING).when(device).getConnectionStatus();
        assertThat(peripheral.isConnected(), is(false));
    }

    @Test
    public void getBondStatus() throws Exception {
        final BluetoothStack stack = mock(BluetoothStack.class);
        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack).when(device).getStack();

        final SensePeripheral peripheral = new SensePeripheral(device);

        doReturn(GattPeripheral.BOND_NONE).when(device).getBondStatus();
        assertThat(GattPeripheral.BOND_NONE, is(equalTo(peripheral.getBondStatus())));

        doReturn(GattPeripheral.BOND_BONDING).when(device).getBondStatus();
        assertThat(GattPeripheral.BOND_BONDING, is(equalTo(peripheral.getBondStatus())));

        doReturn(GattPeripheral.BOND_BONDED).when(device).getBondStatus();
        assertThat(GattPeripheral.BOND_BONDED, is(equalTo(peripheral.getBondStatus())));
    }

    @Test
    public void getScannedRssi() throws Exception {
        final BluetoothStack stack = mock(BluetoothStack.class);
        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack).when(device).getStack();
        doReturn(-50).when(device).getScanTimeRssi();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat(-50, is(equalTo(peripheral.getScannedRssi())));
    }

    @Test
    public void getAddress() throws Exception {
        final BluetoothStack stack = mock(BluetoothStack.class);
        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack).when(device).getStack();
        doReturn("ca:15:4f:fa:b7:0b").when(device).getAddress();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat("ca:15:4f:fa:b7:0b", is(equalTo(peripheral.getAddress())));
    }

    @Test
    public void getName() throws Exception {
        final BluetoothStack stack = mock(BluetoothStack.class);
        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack).when(device).getStack();
        doReturn("Sense-Test").when(device).getName();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat("Sense-Test", is(equalTo(peripheral.getName())));
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
