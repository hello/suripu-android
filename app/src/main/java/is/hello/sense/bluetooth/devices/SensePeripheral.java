package is.hello.sense.bluetooth.devices;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.protobuf.ByteString;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.devices.transmission.SensePacketDataHandler;
import is.hello.sense.bluetooth.devices.transmission.SensePacketHandler;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralBusyError;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.bluetooth.errors.PeripheralSetWifiError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.transmission.PacketDataHandler;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.Bytes;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.functional.Functions;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.MorpheusCommand;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.MorpheusCommand.CommandType;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_connection_state;

public final class SensePeripheral extends HelloPeripheral<SensePeripheral> {
    //region Versions

    /**
     * The command version used by the firmware hasn't been determined yet.
     */
    public static final int VERSION_UNKNOWN = -1;

    /**
     * The command version used by the firmware on the original PVT units.
     */
    public static final int VERSION_PVT = 0;

    /**
     * The command version used by the firmware that
     * is able to parse WEP keys from ASCII strings.
     */
    public static final int VERSION_WEP_FIX = 1;

    @IntDef({VERSION_UNKNOWN, VERSION_PVT, VERSION_WEP_FIX})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Version {}

    //endregion


    private static final long STACK_OPERATION_TIMEOUT_S = 30;
    private static final long SIMPLE_COMMAND_TIMEOUT_S = 45;
    private static final long ANIMATION_TIMEOUT_S = 45;
    private static final long PAIR_PILL_TIMEOUT_S = 90; // Per Pang
    private static final long SET_WIFI_TIMEOUT_S = 90;
    private static final long WIFI_SCAN_TIMEOUT_S = 30;

    private final PacketDataHandler<MorpheusCommand> dataHandler;
    private final PacketHandler packetHandler;

    private @Version int version = VERSION_UNKNOWN;

    public static Observable<List<SensePeripheral>> discover(@NonNull BluetoothStack bluetoothStack,
                                                             @NonNull PeripheralCriteria criteria) {
        criteria.addExactMatchPredicate(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        return bluetoothStack.discoverPeripherals(criteria).map(SensePeripheral::fromDevices);
    }

    public static Observable<SensePeripheral> rediscover(@NonNull BluetoothStack bluetoothStack,
                                                         @NonNull String deviceId,
                                                         boolean includeHighPowerPreScan) {
        PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.setLimit(1);
        criteria.setWantsHighPowerPreScan(includeHighPowerPreScan);
        criteria.addExactMatchPredicate(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        criteria.addStartsWithPredicate(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + deviceId);
        return discover(bluetoothStack, criteria).flatMap(ds -> {
            if (ds.isEmpty()) {
                return Observable.error(new PeripheralNotFoundError());
            } else {
                return Observable.just(ds.get(0));
            }
        });
    }

    static List<SensePeripheral> fromDevices(@NonNull List<Peripheral> peripherals) {
        List<SensePeripheral> mapped = new ArrayList<>();
        for (Peripheral peripheral : peripherals) {
            mapped.add(new SensePeripheral(peripheral));
        }
        return mapped;
    }

    public SensePeripheral(@NonNull Peripheral peripheral) {
        super(peripheral);

        this.dataHandler = new SensePacketDataHandler();
        this.packetHandler = new SensePacketHandler();
        packetHandler.setPacketDataHandler(dataHandler);
        peripheral.setPacketHandler(packetHandler);
    }

    public Observable<ConnectStatus> connect() {
        return super.connect(createOperationTimeout("Connect"));
    }

    //region Versions

    private void setFirmwareVersion(int firmwareVersion) {
        this.version = firmwareVersion;
    }

    private int getCommandVersion() {
        if (version == VERSION_UNKNOWN) {
            return VERSION_PVT;
        } else {
            return version;
        }
    }

    /**
     * Returns the client-consumable firmware version of the peripheral, if known.
     */
    public @Version int getFirmwareVersion() {
        return version;
    }

    //endregion

    //region Internal

    @Override
    protected UUID getTargetServiceIdentifier() {
        return SenseIdentifiers.SERVICE;
    }

    @Override
    protected UUID getDescriptorIdentifier() {
        return SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG;
    }

    protected @NonNull OperationTimeout createOperationTimeout(@NonNull String name) {
        return peripheral.createOperationTimeout(name, STACK_OPERATION_TIMEOUT_S, TimeUnit.SECONDS);
    }

    protected @NonNull OperationTimeout createSimpleCommandTimeout() {
        return peripheral.createOperationTimeout("Simple Command", SIMPLE_COMMAND_TIMEOUT_S, TimeUnit.SECONDS);
    }

    protected @NonNull OperationTimeout createScanWifiTimeout() {
        return peripheral.createOperationTimeout("Scan Wifi", WIFI_SCAN_TIMEOUT_S, TimeUnit.SECONDS);
    }

    protected @NonNull OperationTimeout createPairPillTimeout() {
        return peripheral.createOperationTimeout("Pair Pill", PAIR_PILL_TIMEOUT_S, TimeUnit.SECONDS);
    }

    protected @NonNull OperationTimeout createAnimationTimeout() {
        return peripheral.createOperationTimeout("Animation", ANIMATION_TIMEOUT_S, TimeUnit.SECONDS);
    }

    protected PeripheralBusyError busyError() {
        return new PeripheralBusyError();
    }

    protected boolean isBusy() {
        return dataHandler.hasListeners();
    }

    Observable<MorpheusCommand> performCommand(@NonNull MorpheusCommand command,
                                               @NonNull OperationTimeout timeout,
                                               @NonNull OnCommandResponse onCommandResponse) {
        return peripheral.getStack().newConfiguredObservable(s -> {
            if (isBusy()) {
                s.onError(busyError());
                return;
            }

            timeout.setTimeoutAction(() -> {
                Logger.error(Peripheral.LOG_TAG, "Command timed out " + command);

                dataHandler.clearListeners();

                MorpheusCommand timeoutResponse = MorpheusCommand.newBuilder()
                        .setVersion(getCommandVersion())
                        .setType(CommandType.MORPHEUS_COMMAND_ERROR)
                        .setError(SenseCommandProtos.ErrorType.TIME_OUT)
                        .build();
                onCommandResponse.onResponse(timeoutResponse, s, timeout);
            }, peripheral.getStack().getScheduler());

            Action1<Throwable> onError = error -> {
                timeout.unschedule();
                s.onError(error);
            };
            Observable<UUID> subscribe = subscribe(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, createOperationTimeout("Subscribe"));
            subscribe.subscribe(subscribedCharacteristic -> {
                dataHandler.onResponse = response -> {
                    Logger.info(Peripheral.LOG_TAG, "Got response to command " + command + ": " + response);
                    setFirmwareVersion(response.getVersion());
                    onCommandResponse.onResponse(response, s, timeout);
                };
                dataHandler.onError = dataError -> {
                    timeout.unschedule();

                    if (dataError instanceof BluetoothConnectionLostError || !isConnected()) {
                        onError.call(dataError);
                    } else {
                        Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, createOperationTimeout("Unsubscribe"));
                        Logger.error(Peripheral.LOG_TAG, "Could not complete command " + command, dataError);
                        unsubscribe.subscribe(ignored -> onError.call(dataError), onError);
                    }
                };

                Logger.info(Peripheral.LOG_TAG, "Writing command " + command);

                final byte[] commandData = command.toByteArray();
                Observable<Void> write = writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND, commandData);
                write.subscribe(ignored -> {
                    Logger.info(Peripheral.LOG_TAG, "Wrote command " + command);
                    timeout.schedule();
                }, onError);
            }, onError);
        });
    }

    Observable<MorpheusCommand> performSimpleCommand(@NonNull MorpheusCommand command,
                                                     @NonNull OperationTimeout commandTimeout) {
        return performCommand(command, commandTimeout, (response, s, timeout) -> {
            timeout.unschedule();

            Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, createOperationTimeout("Unsubscribe"));
            unsubscribe.subscribe(ignored -> {
                if (response.getType() == command.getType()) {
                    s.onNext(response);
                    s.onCompleted();
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                    if (response.getError() == SenseCommandProtos.ErrorType.TIME_OUT) {
                        s.onError(new OperationTimeoutError(OperationTimeoutError.Operation.COMMAND_RESPONSE));
                    } else {
                        s.onError(new SensePeripheralError(response.getError()));
                    }
                } else {
                    s.onError(new BluetoothError("Unexpected response " + response.getType()));
                }

                dataHandler.clearListeners();
            }, s::onError);
        });
    }

    //endregion

    //region Operations

    Observable<Void> writeLargeCommand(UUID commandUUID, byte[] commandData) {
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
                        peripheral.writeCommand(peripheralService, commandUUID, remainingPackets.getFirst(), createOperationTimeout("Write Partial Command")).subscribe(this);
                    }
                }
            };
            Logger.info(Peripheral.LOG_TAG, "Writing first chunk of large command (" + remainingPackets.size() + " chunks) " + commandUUID);
            peripheral.writeCommand(peripheralService, commandUUID, remainingPackets.getFirst(), createOperationTimeout("Write Partial Command")).subscribe(writeObserver);
        });
    }

    public Observable<Void> setPairingModeEnabled(boolean enabled) {
        Logger.info(Peripheral.LOG_TAG, "setPairingModeEnabled(" + enabled + ")");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        CommandType commandType;
        if (enabled)
            commandType = CommandType.MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE;
        else
            commandType = CommandType.MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE;

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(commandType)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(Functions.TO_VOID);
    }

    public Observable<Void> clearPairedPhone() {
        Logger.info(Peripheral.LOG_TAG, "clearPairedPhone()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_ERASE_PAIRED_PHONE)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(Functions.TO_VOID);
    }

    public Observable<Void> setWifiNetwork(String bssid,
                                           String ssid,
                                           SenseCommandProtos.wifi_endpoint.sec_type securityType,
                                           String password) {
        Logger.info(Peripheral.LOG_TAG, "setWifiNetwork(" + ssid + ")");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        if (securityType != SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN &&
                TextUtils.isEmpty(password)) {
            return Observable.error(new PeripheralSetWifiError(PeripheralSetWifiError.Reason.EMPTY_PASSWORD));
        }

        int version = getCommandVersion();
        MorpheusCommand.Builder builder = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(version)
                .setWifiName(bssid)
                .setWifiSSID(ssid)
                .setSecurityType(securityType);
        if (version == VERSION_PVT && securityType == SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WEP) {
            byte[] keyBytes = Bytes.tryFromString(password);
            if (keyBytes == null) {
                return Observable.error(new PeripheralSetWifiError(PeripheralSetWifiError.Reason.MALFORMED_BYTES));
            } else if (Bytes.contains(keyBytes, (byte) 0x0)) {
                return Observable.error(new PeripheralSetWifiError(PeripheralSetWifiError.Reason.CONTAINS_NUL_BYTE));
            }
            ByteString keyString = ByteString.copyFrom(keyBytes);
            builder.setWifiPasswordBytes(keyString);
        } else {
            builder.setWifiPassword(password);
        }

        MorpheusCommand morpheusCommand = builder.build();
        return performSimpleCommand(morpheusCommand, peripheral.createOperationTimeout("Set Wifi", SET_WIFI_TIMEOUT_S, TimeUnit.SECONDS)).map(Functions.TO_VOID);
    }

    public Observable<SenseWifiNetwork> getWifiNetwork() {
        Logger.info(Peripheral.LOG_TAG, "getWifiNetwork()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_GET_WIFI_ENDPOINT)
                .setVersion(getCommandVersion())
                .build();

        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(response ->
                new SenseWifiNetwork(response.getWifiSSID(), response.getWifiConnectionState()));
    }

    public Observable<String> pairPill(final String accountToken) {
        Logger.info(Peripheral.LOG_TAG, "pairPill(" + accountToken + ")");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_PILL)
                .setVersion(getCommandVersion())
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand, createPairPillTimeout()).map(MorpheusCommand::getDeviceId);
    }

    public Observable<Void> linkAccount(final String accountToken) {
        Logger.info(Peripheral.LOG_TAG, "linkAccount(" + accountToken + ")");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(getCommandVersion())
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(Functions.TO_VOID);
    }

    public Observable<Void> factoryReset() {
        Logger.info(Peripheral.LOG_TAG, "factoryReset()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_FACTORY_RESET)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(Functions.TO_VOID);
    }

    public Observable<Void> pushData() {
        Logger.info(Peripheral.LOG_TAG, "pushData()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PUSH_DATA_AFTER_SET_TIMEZONE)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(Functions.TO_VOID);
    }

    public Observable<Void> runLedAnimation(@NonNull LedAnimation animationType) {
        Logger.info(Peripheral.LOG_TAG, "runLedAnimation(" + animationType + ")");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(animationType.commandType)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createAnimationTimeout()).map(Functions.TO_VOID);
    }

    public Observable<Void> startWifiScan() {
        Logger.info(Peripheral.LOG_TAG, "startWifiScan()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SCAN_WIFI)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(Functions.TO_VOID);
    }

    public Observable<SenseCommandProtos.wifi_endpoint> nextWifiEndpoint() {
        Logger.info(Peripheral.LOG_TAG, "nextWifiEndpoint()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SCAN_WIFI)
                .setVersion(getCommandVersion())
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(r -> r.getWifiScanResult(0));
    }

    public Observable<List<SenseCommandProtos.wifi_endpoint>> scanForWifiNetworks() {
        Logger.info(Peripheral.LOG_TAG, "scanForWifiNetworks()");

        if (isBusy()) {
            return Observable.error(busyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_START_WIFISCAN)
                .setVersion(getCommandVersion())
                .build();

        //noinspection MismatchedQueryAndUpdateOfCollection
        List<SenseCommandProtos.wifi_endpoint> endpoints = new ArrayList<>();
        return performCommand(morpheusCommand, createScanWifiTimeout(), (response, subscriber, timeout) -> {
            Action1<Throwable> onError = subscriber::onError;

            if (response.getType() == CommandType.MORPHEUS_COMMAND_START_WIFISCAN) {
                timeout.reschedule();

                if(response.getWifiScanResultCount() == 1) {
                    endpoints.add(response.getWifiScanResult(0));
                }
            } else if (response.getType() == CommandType.MORPHEUS_COMMAND_STOP_WIFISCAN) {
                timeout.unschedule();

                Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, createOperationTimeout("Unsubscribe"));
                unsubscribe.subscribe(ignored -> {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                }, onError);

                dataHandler.clearListeners();
            } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                timeout.unschedule();

                Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, createOperationTimeout("Unsubscribe"));
                unsubscribe.subscribe(ignored -> {
                    if (response.getError() == SenseCommandProtos.ErrorType.TIME_OUT) {
                        onError.call(new OperationTimeoutError(OperationTimeoutError.Operation.COMMAND_RESPONSE));
                    } else {
                        onError.call(new SensePeripheralError(response.getError()));
                    }
                }, e -> {
                    if (response.getError() == SenseCommandProtos.ErrorType.TIME_OUT) {
                        onError.call(new OperationTimeoutError(OperationTimeoutError.Operation.COMMAND_RESPONSE, e));
                    } else {
                        onError.call(new SensePeripheralError(response.getError(), e));
                    }
                });

                dataHandler.clearListeners();
            } else {
                timeout.unschedule();

                Observable<UUID> unsubscribe = unsubscribe(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, createOperationTimeout("Unsubscribe"));
                unsubscribe.subscribe(ignored -> onError.call(new SensePeripheralError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR)), onError);

                dataHandler.clearListeners();
            }
        }).map(ignored -> endpoints);
    }

    //endregion


    interface OnCommandResponse {
        void onResponse(MorpheusCommand response, Subscriber<? super MorpheusCommand> subscriber, OperationTimeout timeout);
    }


    public final class SenseWifiNetwork {
        public final @Nullable String ssid;
        public final @Nullable wifi_connection_state connectionState;

        SenseWifiNetwork(@Nullable String ssid, @Nullable wifi_connection_state connectionState) {
            this.ssid = ssid;
            this.connectionState = connectionState;
        }


        @Override
        public String toString() {
            return "SenseWifiNetwork{" +
                    "ssid='" + ssid + '\'' +
                    ", connectionState=" + connectionState +
                    ", isBusy=" + isBusy() +
                    '}';
        }
    }

    public static enum LedAnimation {
        BUSY(CommandType.MORPHEUS_COMMAND_LED_BUSY),
        TRIPPY(CommandType.MORPHEUS_COMMAND_LED_TRIPPY),
        FADE_OUT(CommandType.MORPHEUS_COMMAND_LED_OPERATION_SUCCESS),
        STOP(CommandType.MORPHEUS_COMMAND_LED_OPERATION_FAILED);

        final CommandType commandType;

        LedAnimation(@NonNull CommandType commandType) {
            this.commandType = commandType;
        }
    }
}
