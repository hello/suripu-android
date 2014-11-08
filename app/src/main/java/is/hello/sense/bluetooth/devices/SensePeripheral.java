package is.hello.sense.bluetooth.devices;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.devices.transmission.SensePacketDataHandler;
import is.hello.sense.bluetooth.devices.transmission.SensePacketHandler;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.GattError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.util.ScanResponse;
import is.hello.sense.bluetooth.stacks.util.ScanCriteria;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.SchedulerOperationTimeout;
import is.hello.sense.bluetooth.stacks.transmission.PacketDataHandler;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Action3;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle.MorpheusCommand;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle.MorpheusCommand.CommandType;

public class SensePeripheral extends HelloPeripheral<SensePeripheral> {
    private static int COMMAND_VERSION = 0;

    private static final long STACK_TIMEOUT_S = 30;
    private static final long WIFI_SCAN_TIMEOUT_S = 60;

    private final OperationTimeout simpleCommandTimeout = new SchedulerOperationTimeout("Simple Command", 45, TimeUnit.SECONDS);
    private final PacketDataHandler<MorpheusCommand> dataHandler;
    private final PacketHandler packetHandler;

    public static Observable<List<SensePeripheral>> discover(@NonNull BluetoothStack bluetoothStack,
                                                             @NonNull ScanCriteria criteria) {
        criteria.addConstraint(new ScanResponse(ScanResponse.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT));
        return bluetoothStack.discoverPeripherals(criteria)
                             .map(SensePeripheral::fromDevices);
    }

    public static Observable<SensePeripheral> rediscover(@NonNull BluetoothStack bluetoothStack,
                                                         @NonNull String deviceId) {
        ScanCriteria criteria = new ScanCriteria();
        criteria.setLimit(1);
        criteria.addConstraint(new ScanResponse(ScanResponse.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + deviceId));
        return discover(bluetoothStack, criteria).map(ds -> ds.get(0));
    }

    public static List<SensePeripheral> fromDevices(@NonNull List<Peripheral> peripherals) {
        List<SensePeripheral> mapped = new ArrayList<>();
        for (Peripheral peripheral : peripherals) {
            mapped.add(new SensePeripheral(peripheral));
        }
        return mapped;
    }

    public SensePeripheral(@NonNull Peripheral peripheral) {
        super(peripheral, new SchedulerOperationTimeout("Common Peripheral", STACK_TIMEOUT_S, TimeUnit.SECONDS));

        this.dataHandler = new SensePacketDataHandler();
        this.packetHandler = new SensePacketHandler();
        packetHandler.setPacketDataHandler(dataHandler);
        peripheral.setPacketHandler(packetHandler);
    }


    //region Internal


    @Override
    protected UUID getTargetServiceIdentifier() {
        return SenseIdentifiers.SERVICE;
    }

    protected PeripheralService getTargetService() {
        return peripheralService;
    }

    @Override
    protected UUID getDescriptorIdentifier() {
        return SenseIdentifiers.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG;
    }

    protected Observable<MorpheusCommand> performCommand(@NonNull MorpheusCommand command,
                                                         @NonNull OperationTimeout timeout,
                                                         @NonNull OnCommandResponse onCommandResponse) {
        return peripheral.getStack().newConfiguredObservable(s -> {
            timeout.setTimeoutAction(() -> {
                Logger.error(Peripheral.LOG_TAG, "Command timed out " + command);

                dataHandler.onResponse = null;
                dataHandler.onError = null;

                MorpheusCommand timeoutResponse = MorpheusCommand.newBuilder()
                        .setVersion(COMMAND_VERSION)
                        .setType(CommandType.MORPHEUS_COMMAND_ERROR)
                        .setError(MorpheusBle.ErrorType.TIME_OUT)
                        .build();
                onCommandResponse.call(timeoutResponse, s, timeout);
            }, peripheral.getStack().getScheduler());

            Action1<Throwable> onError = s::onError;
            Observable<UUID> subscribe = subscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
            subscribe.subscribe(subscribedCharacteristic -> {
                dataHandler.onResponse = response -> {
                    Logger.info(Peripheral.LOG_TAG, "Got response to command " + command + ": " + response);
                    onCommandResponse.call(response, s, timeout);
                };
                dataHandler.onError = dataError -> {
                    timeout.unschedule();
                    timeout.recycle();

                    Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
                    Logger.error(Peripheral.LOG_TAG, "Could not complete command " + command, dataError);
                    unsubscribe.subscribe(ignored -> s.onError(dataError), onError);
                };

                Logger.info(Peripheral.LOG_TAG, "Writing command " + command);

                final byte[] commandData = command.toByteArray();
                Observable<Void> write = writeLargeCommand(SenseIdentifiers.CHAR_PROTOBUF_COMMAND, commandData);
                write.subscribe(ignored -> {
                    Logger.info(Peripheral.LOG_TAG, "Wrote command " + command);
                    timeout.schedule();
                }, onError);
            }, onError);
        });
    }

    protected Observable<MorpheusCommand> performSimpleCommand(@NonNull MorpheusCommand command) {
        return performCommand(command, simpleCommandTimeout, (response, s, timeout) -> {
            timeout.unschedule();
            timeout.recycle();

            Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
            unsubscribe.subscribe(ignored -> {
                if (response.getType() == command.getType()) {
                    s.onNext(response);
                    s.onCompleted();
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                    s.onError(new SensePeripheralError(response.getError()));
                } else {
                    s.onError(new BluetoothError());
                }
            }, s::onError);
        });
    }

    //endregion

    //region Operations

    private Observable<Void> writeLargeCommand(UUID commandUUID, byte[] commandData) {
        List<byte[]> blePackets = packetHandler.createPackets(commandData);
        LinkedList<byte[]> remainingPackets = new LinkedList<>(blePackets);

        return Observable.create((Observable.OnSubscribe<Void>) s -> {
            Observer<Void> writeObserver = new Observer<Void>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }

                @Override
                public void onNext(Void ignored) {
                    remainingPackets.removeFirst();
                    if (remainingPackets.isEmpty()) {
                        Logger.info(Peripheral.LOG_TAG, "Write large command " + commandUUID);

                        s.onNext(null);
                        s.onCompleted();
                    } else {
                        Logger.info(Peripheral.LOG_TAG, "Writing next chunk of large command " + commandUUID);
                        peripheral.writeCommand(peripheralService, commandUUID, remainingPackets.getFirst(), commonTimeout).subscribe(this);
                    }
                }
            };
            Logger.info(Peripheral.LOG_TAG, "Writing first chunk of large command (" + remainingPackets.size() + " chunks) " + commandUUID);
            peripheral.writeCommand(peripheralService, commandUUID, remainingPackets.getFirst(), commonTimeout).subscribe(writeObserver);
        });
    }

    public Observable<Void> setPairingModeEnabled(boolean enabled) {
        Logger.info(Peripheral.LOG_TAG, "setPairingModeEnabled(" + enabled + ")");

        CommandType commandType;
        if (enabled)
            commandType = CommandType.MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE;
        else
            commandType = CommandType.MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE;

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(commandType)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> clearPairedPhone() {
        Logger.info(Peripheral.LOG_TAG, "clearPairedPhone()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_EREASE_PAIRED_PHONE)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> beginDfu() {
        Logger.info(Peripheral.LOG_TAG, "beginDfu()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_MORPHEUS_DFU_BEGIN)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> setWifiNetwork(String bssid,
                                           String ssid,
                                           MorpheusBle.wifi_endpoint.sec_type securityType,
                                           String password) {
        Logger.info(Peripheral.LOG_TAG, "setWifiNetwork(" + ssid + ")");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(COMMAND_VERSION)
                .setWifiName(bssid)
                .setWifiSSID(ssid)
                .setWifiPassword(password)
                .setSecurityType(securityType)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<String> pairPill(final String accountToken) {
        Logger.info(Peripheral.LOG_TAG, "pairPill(" + accountToken + ")");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_PILL)
                .setVersion(COMMAND_VERSION)
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand).map(MorpheusCommand::getDeviceId);
    }

    public Observable<Void> linkAccount(final String accountToken) {
        Logger.info(Peripheral.LOG_TAG, "linkAccount(" + accountToken + ")");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(COMMAND_VERSION)
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> factoryReset() {
        Logger.info(Peripheral.LOG_TAG, "factoryReset()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_FACTORY_RESET)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<List<MorpheusBle.wifi_endpoint>> scanForWifiNetworks() {
        Logger.info(Peripheral.LOG_TAG, "scanForWifiNetworks()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_START_WIFISCAN)
                .setVersion(COMMAND_VERSION)
                .build();

        //noinspection MismatchedQueryAndUpdateOfCollection
        List<MorpheusBle.wifi_endpoint> endpoints = new ArrayList<>();
        Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
        OperationTimeout wifiTimeout = new SchedulerOperationTimeout("Scan Wifi", WIFI_SCAN_TIMEOUT_S, TimeUnit.SECONDS);
        return performCommand(morpheusCommand, wifiTimeout, (response, subscriber, timeout) -> {
            if (response.getType() == CommandType.MORPHEUS_COMMAND_START_WIFISCAN) {
                if(response.getWifiScanResultCount() == 1) {
                    endpoints.add(response.getWifiScanResult(0));
                }
            } else if (response.getType() == CommandType.MORPHEUS_COMMAND_STOP_WIFISCAN) {
                timeout.unschedule();

                unsubscription.subscribe(ignored -> {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                }, subscriber::onError);
            } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                timeout.unschedule();

                unsubscription.subscribe(ignored -> subscriber.onError(new SensePeripheralError(response.getError())), subscriber::onError);
            } else {
                timeout.unschedule();

                unsubscription.subscribe(ignored -> subscriber.onError(new GattError(0)), subscriber::onError);
            }
        }).map(ignored -> endpoints);
    }

    public Observable<String> deviceId() {
        Logger.info(Peripheral.LOG_TAG, "deviceId()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_GET_DEVICE_ID)
                .setVersion(COMMAND_VERSION)
                .build();

        return performSimpleCommand(morpheusCommand).map(MorpheusCommand::getDeviceId);
    }

    //endregion


    private interface OnCommandResponse extends Action3<MorpheusCommand, Subscriber<? super MorpheusCommand>, OperationTimeout> {}
}
