package is.hello.sense.bluetooth.sense;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.BluetoothDisabledError;
import is.hello.buruberi.bluetooth.errors.BluetoothGattError;
import is.hello.buruberi.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.buruberi.bluetooth.errors.PeripheralConnectionError;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.AdvertisingDataBuilder;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SensePeripheralTests extends SenseTestCase {
    private static final String TEST_DEVICE_ID = "CA154FFA";

    //region Vending Mocks

    static BluetoothStack createMockBluetoothStack() {
        BluetoothStack stack = mock(BluetoothStack.class);
        doReturn(Schedulers.immediate())
                .when(stack)
                .getScheduler();
        doReturn(mock(LoggerFacade.class, CALLS_REAL_METHODS))
                .when(stack)
                .getLogger();
        return stack;
    }

    static GattPeripheral createMockPeripheral(@NonNull BluetoothStack stack) {
        final GattPeripheral device = mock(GattPeripheral.class);
        doReturn(stack)
                .when(device)
                .getStack();
        return device;
    }

    static PeripheralService createMockPeripheralService() {
        PeripheralService peripheralService = mock(PeripheralService.class);
        doReturn(SenseIdentifiers.SERVICE)
                .when(peripheralService)
                .getUuid();
        doReturn(PeripheralService.SERVICE_TYPE_PRIMARY)
                .when(peripheralService)
                .getType();
        return peripheralService;
    }

    //endregion


    //region Discovery

    @Test
    public void discovery() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();

        final AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                    SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        final AdvertisingData advertisingData = builder.build();

        final GattPeripheral device1 = createMockPeripheral(stack);
        doReturn("Sense-Test").when(device1).getName();
        doReturn("ca:15:4f:fa:b7:0b").when(device1).getAddress();
        doReturn(-50).when(device1).getScanTimeRssi();
        doReturn(advertisingData).when(device1).getAdvertisingData();

        final GattPeripheral device2 = createMockPeripheral(stack);
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
        final BluetoothStack stack = createMockBluetoothStack();

        final AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        final AdvertisingData advertisingData = builder.build();

        final GattPeripheral device = createMockPeripheral(stack);
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


    //region Attributes

    @Test
    public void isConnected() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(GattPeripheral.STATUS_CONNECTED).when(device).getConnectionStatus();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat(peripheral.isConnected(), is(false));

        peripheral.peripheralService = createMockPeripheralService();
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
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
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
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(-50).when(device).getScanTimeRssi();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat(-50, is(equalTo(peripheral.getScannedRssi())));
    }

    @Test
    public void getAddress() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn("ca:15:4f:fa:b7:0b").when(device).getAddress();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat("ca:15:4f:fa:b7:0b", is(equalTo(peripheral.getAddress())));
    }

    @Test
    public void getName() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn("Sense-Test").when(device).getName();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertThat("Sense-Test", is(equalTo(peripheral.getName())));
    }

    @Test
    public void getDeviceId() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                    SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA,
                    SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        AdvertisingData advertisingData = builder.build();

        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(advertisingData)
                .when(device)
                .getAdvertisingData();

        final SensePeripheral peripheral = new SensePeripheral(device);
        assertEquals(TEST_DEVICE_ID, peripheral.getDeviceId());
    }

    //endregion


    //region Connectivity

    @Test
    public void connectSucceeded() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(Observable.just(device))
                .when(device)
                .connect(any(OperationTimeout.class));
        doReturn(Observable.just(device))
                .when(device)
                .createBond();
        doReturn(Observable.just(mock(PeripheralService.class)))
                .when(device)
                .discoverService(eq(SenseIdentifiers.SERVICE), any(OperationTimeout.class));

        final SensePeripheral peripheral = spy(new SensePeripheral(device));

        Sync.last(peripheral.connect());

        verify(device).connect(any(OperationTimeout.class));
        verify(device).createBond();
        verify(device).discoverService(eq(SenseIdentifiers.SERVICE), any(OperationTimeout.class));
    }

    @Test
     public void connectFailedFromConnect() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(Observable.error(new BluetoothDisabledError()))
                .when(device)
                .connect(any(OperationTimeout.class));
        doReturn(Observable.just(device))
                .when(device)
                .createBond();
        doReturn(Observable.just(mock(PeripheralService.class)))
                .when(device)
                .discoverService(eq(SenseIdentifiers.SERVICE), any(OperationTimeout.class));
        doReturn(Observable.just(device))
                .when(device)
                .disconnect();

        final SensePeripheral peripheral = new SensePeripheral(device);

        Sync.wrap(peripheral.connect())
            .assertThrows(BluetoothDisabledError.class);
    }

    @Test
    public void connectFailedFromCreateBond() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(Observable.error(new BluetoothDisabledError()))
                .when(device)
                .connect(any(OperationTimeout.class));
        doReturn(Observable.error(new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_ANDROID_API_CHANGED)))
                .when(device)
                .createBond();
        doReturn(Observable.just(mock(PeripheralService.class)))
                .when(device)
                .discoverService(eq(SenseIdentifiers.SERVICE), any(OperationTimeout.class));
        doReturn(Observable.just(device))
                .when(device)
                .disconnect();

        final SensePeripheral peripheral = new SensePeripheral(device);

        Sync.wrap(peripheral.connect())
            .assertThrows(PeripheralBondAlterationError.class);
    }

    @Test
    public void connectFailedFromDiscoverService() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(Observable.error(new BluetoothDisabledError()))
                .when(device)
                .connect(any(OperationTimeout.class));
        doReturn(Observable.just(device))
                .when(device)
                .createBond();
        doReturn(Observable.error(new BluetoothGattError(BluetoothGatt.GATT_FAILURE,
                                                         BluetoothGattError.Operation.DISCOVER_SERVICES)))
                .when(device)
                .discoverService(eq(SenseIdentifiers.SERVICE), any(OperationTimeout.class));
        doReturn(Observable.just(device))
                .when(device)
                .disconnect();

        final SensePeripheral peripheral = new SensePeripheral(device);

        Sync.wrap(peripheral.connect())
            .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void disconnectSuccess() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(Observable.just(device))
                .when(device)
                .disconnect();

        final SensePeripheral peripheral = new SensePeripheral(device);

        Sync.wrap(peripheral.disconnect())
            .assertThat(is(equalTo(peripheral)));
    }

    @Test
    public void disconnectFailure() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);
        doReturn(Observable.error(new BluetoothGattError(BluetoothGatt.GATT_FAILURE,
                                                         BluetoothGattError.Operation.DISCONNECT)))
                .when(device)
                .disconnect();

        final SensePeripheral peripheral = new SensePeripheral(device);

        Sync.wrap(peripheral.disconnect())
            .assertThrows(BluetoothGattError.class);
    }

    //endregion


    //region Subscriptions

    @Test
    public void subscribeResponseSuccess() throws Exception {
        final UUID characteristicId = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        final UUID descriptorId = SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG;

        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(GattPeripheral.STATUS_CONNECTED)
                .when(device)
                .getConnectionStatus();
        doReturn(Observable.just(characteristicId))
                .when(device)
                .enableNotification(any(PeripheralService.class),
                                    eq(characteristicId),
                                    eq(descriptorId),
                                    any(OperationTimeout.class));

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        Sync.wrap(peripheral.subscribeResponse(mock(OperationTimeout.class)))
            .assertThat(is(equalTo(characteristicId)));


        doReturn(Observable.error(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION,
                                                         BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)))
                .when(device)
                .enableNotification(any(PeripheralService.class),
                                    eq(characteristicId),
                                    eq(descriptorId),
                                    any(OperationTimeout.class));

        Sync.wrap(peripheral.subscribeResponse(mock(OperationTimeout.class)))
                .assertThrows(BluetoothGattError.class);


        doReturn(GattPeripheral.STATUS_DISCONNECTED)
                .when(device)
                .getConnectionStatus();

        Sync.wrap(peripheral.subscribeResponse(mock(OperationTimeout.class)))
                .assertThrows(PeripheralConnectionError.class);
    }

    @Test
    public void subscribeResponseFailure() throws Exception {
        final UUID characteristicId = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        final UUID descriptorId = SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG;

        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(GattPeripheral.STATUS_CONNECTED)
                .when(device)
                .getConnectionStatus();
        doReturn(Observable.error(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION,
                                                         BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)))
                .when(device)
                .enableNotification(any(PeripheralService.class),
                                    eq(characteristicId),
                                    eq(descriptorId),
                                    any(OperationTimeout.class));

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        Sync.wrap(peripheral.subscribeResponse(mock(OperationTimeout.class)))
            .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void unsubscribeResponseSuccess() throws Exception {
        final UUID characteristicId = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        final UUID descriptorId = SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG;

        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(GattPeripheral.STATUS_CONNECTED)
                .when(device)
                .getConnectionStatus();
        doReturn(Observable.just(characteristicId))
                .when(device)
                .disableNotification(any(PeripheralService.class),
                                     eq(characteristicId),
                                     eq(descriptorId),
                                     any(OperationTimeout.class));

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        Sync.wrap(peripheral.unsubscribeResponse(mock(OperationTimeout.class)))
            .assertThat(is(equalTo(characteristicId)));


        doReturn(Observable.error(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION,
                                                         BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)))
                .when(device)
                .disableNotification(any(PeripheralService.class),
                                     eq(characteristicId),
                                     eq(descriptorId),
                                     any(OperationTimeout.class));

        Sync.wrap(peripheral.unsubscribeResponse(mock(OperationTimeout.class)))
            .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void unsubscribeResponseFailure() throws Exception {
        final UUID characteristicId = SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE;
        final UUID descriptorId = SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG;

        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(GattPeripheral.STATUS_CONNECTED)
                .when(device)
                .getConnectionStatus();
        doReturn(Observable.error(new BluetoothGattError(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION,
                                                         BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION)))
                .when(device)
                .disableNotification(any(PeripheralService.class),
                                     eq(characteristicId),
                                     eq(descriptorId),
                                     any(OperationTimeout.class));

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        Sync.wrap(peripheral.unsubscribeResponse(mock(OperationTimeout.class)))
            .assertThrows(BluetoothGattError.class);
    }

    @Test
    public void unsubscribeResponseNoConnection() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(GattPeripheral.STATUS_DISCONNECTED)
                .when(device)
                .getConnectionStatus();

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        Sync.wrap(peripheral.unsubscribeResponse(mock(OperationTimeout.class)))
            .assertThat(is(equalTo(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE)));
    }

    //endregion


    //region Writing Commands

    @Test
    public void writeLargeCommandSuccess() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(Observable.just(null))
                .when(device)
                .writeCommand(any(PeripheralService.class),
                              eq(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND),
                              any(GattPeripheral.WriteType.class),
                              any(byte[].class),
                              any(OperationTimeout.class));

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        MorpheusCommand command = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(0)
                .setWifiName("Mostly Radiation")
                .setWifiSSID("00:00:00:00:00:00")
                .setSecurityType(SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .build();
        Sync.last(peripheral.writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND,
                                               command.toByteArray()));

        verify(device, times(3)).writeCommand(any(PeripheralService.class),
                                              eq(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND),
                                              any(GattPeripheral.WriteType.class),
                                              any(byte[].class),
                                              any(OperationTimeout.class));
    }

    @Test
    public void writeLargeCommandFailure() throws Exception {
        final BluetoothStack stack = createMockBluetoothStack();
        final GattPeripheral device = createMockPeripheral(stack);

        doReturn(Observable.error(new BluetoothGattError(BluetoothGattError.GATT_STACK_ERROR,
                                                         BluetoothGattError.Operation.WRITE_COMMAND)))
                .when(device)
                .writeCommand(any(PeripheralService.class),
                              eq(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND),
                              any(GattPeripheral.WriteType.class),
                              any(byte[].class),
                              any(OperationTimeout.class));

        final SensePeripheral peripheral = new SensePeripheral(device);
        peripheral.peripheralService = createMockPeripheralService();

        MorpheusCommand command = MorpheusCommand.newBuilder()
                                                 .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                                                 .setVersion(0)
                                                 .setWifiName("Mostly Radiation")
                                                 .setWifiSSID("00:00:00:00:00:00")
                                                 .setSecurityType(SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                                                 .build();
        Sync.wrap(peripheral.writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND,
                                               command.toByteArray()))
            .assertThrows(BluetoothGattError.class);

        verify(device, times(1)).writeCommand(any(PeripheralService.class),
                                              eq(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND),
                                              any(GattPeripheral.WriteType.class),
                                              any(byte[].class),
                                              any(OperationTimeout.class));
    }

    //endregion
}
