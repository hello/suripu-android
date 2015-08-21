package is.hello.sense.bluetooth.sense;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.buruberi.bluetooth.errors.OperationTimeoutError;
import is.hello.buruberi.bluetooth.errors.PeripheralBusyError;
import is.hello.buruberi.bluetooth.errors.PeripheralNotFoundError;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.Operation;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.bluetooth.sense.errors.SenseConnectWifiError;
import is.hello.sense.bluetooth.sense.errors.SensePeripheralError;
import is.hello.sense.bluetooth.sense.errors.SenseSetWifiValidationError;
import is.hello.sense.bluetooth.sense.errors.SenseUnexpectedResponseError;
import is.hello.sense.bluetooth.sense.model.SenseConnectToWiFiUpdate;
import is.hello.sense.bluetooth.sense.model.SenseLedAnimation;
import is.hello.sense.bluetooth.sense.model.SenseNetworkStatus;
import is.hello.sense.bluetooth.sense.model.SensePacketHandler;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.wifi_endpoint;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;

import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand;
import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand.CommandType;
import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.wifi_connection_state;

public class SensePeripheral {
    //region Versions

    /**
     * The command version used by the app.
     */
    public static final int APP_VERSION = 0;

    /**
     * The command version used by the firmware on the original PVT units.
     */
    public static final int COMMAND_VERSION_PVT = 0;

    /**
     * The command version used by the firmware that
     * is able to parse WEP keys from ASCII strings.
     */
    public static final int COMMAND_VERSION_WEP_FIX = 1;

    //endregion


    private static final long STACK_OPERATION_TIMEOUT_S = 30;
    private static final long REMOVE_BOND_TIMEOUT_S = 15;
    private static final long SIMPLE_COMMAND_TIMEOUT_S = 45;
    private static final long ANIMATION_TIMEOUT_S = 45;
    private static final long PAIR_PILL_TIMEOUT_S = 90; // Per Pang
    private static final long SET_WIFI_TIMEOUT_S = 90;
    private static final long WIFI_SCAN_TIMEOUT_S = 30;

    private final GattPeripheral gattPeripheral;
    private final LoggerFacade logger;
    @VisibleForTesting PeripheralService peripheralService;

    private final SensePacketHandler packetHandler;

    private int commandVersion = COMMAND_VERSION_PVT;


    //region Lifecycle

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

    static List<SensePeripheral> fromDevices(@NonNull List<GattPeripheral> peripherals) {
        List<SensePeripheral> mapped = new ArrayList<>();
        for (GattPeripheral gattPeripheral : peripherals) {
            mapped.add(new SensePeripheral(gattPeripheral));
        }
        return mapped;
    }

    public SensePeripheral(@NonNull GattPeripheral gattPeripheral) {
        this.logger = gattPeripheral.getStack().getLogger();
        this.gattPeripheral = gattPeripheral;

        this.packetHandler = new SensePacketHandler();
        gattPeripheral.setPacketHandler(packetHandler);
    }

    //endregion


    //region Connectivity

    /**
     * Connects to the Sense, ensures a bond is present, and performs service discovery.
     */
    public Observable<Operation> connect() {
        Observable<Operation> sequence;

        OperationTimeout timeout = createOperationTimeout("Connect");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Some Lollipop devices (not all!) do not support establishing
            // bonds after connecting. This is the exact opposite of the
            // behavior in KitKat and Gingerbread, which cannot establish
            // bonds without an active connection.
            sequence = Observable.concat(
                Observable.just(Operation.BONDING),
                gattPeripheral.createBond().map(ignored -> Operation.CONNECTING),
                gattPeripheral.connect(timeout).map(ignored -> Operation.DISCOVERING_SERVICES),
                gattPeripheral.discoverService(SenseIdentifiers.SERVICE, timeout).map(service -> {
                    this.peripheralService = service;
                    return Operation.CONNECTED;
                })
            );
        } else {
            sequence = Observable.concat(
                Observable.just(Operation.CONNECTING),
                gattPeripheral.connect(timeout).map(ignored -> Operation.BONDING),
                gattPeripheral.createBond().map(ignored -> Operation.DISCOVERING_SERVICES),
                gattPeripheral.discoverService(SenseIdentifiers.SERVICE, timeout).map(service -> {
                    this.peripheralService = service;
                    return Operation.CONNECTED;
                })
            );
        }

        return sequence.subscribeOn(gattPeripheral.getStack().getScheduler())
                       .doOnNext(s -> logger.info(getClass().getSimpleName(), "is " + s))
                       .doOnError(connectError -> {
                           if (isConnected()) {
                               logger.warn(getClass().getSimpleName(), "Disconnecting after failed connection attempt.", connectError);
                               disconnect().subscribe(ignored -> {
                                   logger.info(getClass().getSimpleName(), "Disconnected after failed connection attempt.");
                               }, disconnectError -> {
                                   logger.error(getClass().getSimpleName(), "Disconnected after failed connection attempt failed, ignoring.", disconnectError);
                               });
                           }
                       });
    }

    public Observable<SensePeripheral> disconnect() {
        return gattPeripheral.disconnect()
                .map(ignored -> this)
                .finallyDo(() -> {
                    this.peripheralService = null;
                });
    }

    public Observable<SensePeripheral> removeBond() {
        OperationTimeout timeout = gattPeripheral.createOperationTimeout("Remove bond", REMOVE_BOND_TIMEOUT_S, TimeUnit.SECONDS);
        return gattPeripheral.removeBond(timeout).map(ignored -> this);
    }

    //endregion


    //region Attributes

    public int getScannedRssi() {
        return gattPeripheral.getScanTimeRssi();
    }

    public String getAddress() {
        return gattPeripheral.getAddress();
    }

    public String getName() {
        return gattPeripheral.getName();
    }

    public boolean isConnected() {
        return (gattPeripheral.getConnectionStatus() == GattPeripheral.STATUS_CONNECTED &&
                peripheralService != null);
    }

    public int getBondStatus() {
        return gattPeripheral.getBondStatus();
    }

    public @Nullable String getDeviceId() {
        AdvertisingData advertisingData = gattPeripheral.getAdvertisingData();
        Collection<byte[]> serviceDataRecords = advertisingData.getRecordsForType(AdvertisingData.TYPE_SERVICE_DATA);
        if (serviceDataRecords != null) {
            byte[] servicePrefix = Bytes.fromString(SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT);
            for (byte[] serviceDataRecord : serviceDataRecords) {
                if (Bytes.startWith(serviceDataRecord, servicePrefix)) {
                    return Bytes.toString(serviceDataRecord, servicePrefix.length, serviceDataRecord.length);
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return '{' + getClass().getSimpleName() + ' ' + getName() + '@' + getAddress() + '}';
    }

    //endregion


    //region Internal

    private @NonNull OperationTimeout createOperationTimeout(@NonNull String name) {
        return gattPeripheral.createOperationTimeout(name, STACK_OPERATION_TIMEOUT_S, TimeUnit.SECONDS);
    }

    private @NonNull OperationTimeout createSimpleCommandTimeout() {
        return gattPeripheral.createOperationTimeout("Simple Command", SIMPLE_COMMAND_TIMEOUT_S, TimeUnit.SECONDS);
    }

    private @NonNull OperationTimeout createScanWifiTimeout() {
        return gattPeripheral.createOperationTimeout("Scan Wifi", WIFI_SCAN_TIMEOUT_S, TimeUnit.SECONDS);
    }

    private @NonNull OperationTimeout createPairPillTimeout() {
        return gattPeripheral.createOperationTimeout("Pair Pill", PAIR_PILL_TIMEOUT_S, TimeUnit.SECONDS);
    }

    private @NonNull OperationTimeout createAnimationTimeout() {
        return gattPeripheral.createOperationTimeout("Animation", ANIMATION_TIMEOUT_S, TimeUnit.SECONDS);
    }

    private PeripheralBusyError createBusyError() {
        return new PeripheralBusyError();
    }

    private boolean isBusy() {
        return packetHandler.hasResponseListener();
    }

    @VisibleForTesting
    Observable<UUID> subscribeResponse(@NonNull OperationTimeout timeout) {
        return gattPeripheral.enableNotification(peripheralService,
                SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE,
                SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG,
                timeout);
    }

    @VisibleForTesting
    Observable<UUID> unsubscribeResponse(@NonNull OperationTimeout timeout) {
        if (isConnected()) {
            return gattPeripheral.disableNotification(peripheralService,
                    SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE,
                    SenseIdentifiers.DESCRIPTOR_CHARACTERISTIC_COMMAND_RESPONSE_CONFIG,
                    timeout);
        } else {
            return Observable.just(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE);
        }
    }

    private <T> Observable<T> performCommand(@NonNull MorpheusCommand command,
                                             @NonNull OperationTimeout timeout,
                                             @NonNull ResponseHandler<T> responseHandler) {
        return gattPeripheral.getStack().newConfiguredObservable(subscriber -> {
            responseHandler.configure(subscriber, timeout);

            if (isBusy()) {
                responseHandler.onError(createBusyError());
                return;
            }

            timeout.setTimeoutAction(() -> {
                logger.error(GattPeripheral.LOG_TAG, "Command timed out " + command, null);

                packetHandler.setResponseListener(null);

                MorpheusCommand timeoutResponse = MorpheusCommand.newBuilder()
                        .setVersion(commandVersion)
                        .setType(CommandType.MORPHEUS_COMMAND_ERROR)
                        .setError(SenseCommandProtos.ErrorType.TIME_OUT)
                        .build();
                responseHandler.onResponse(timeoutResponse);
            }, gattPeripheral.getStack().getScheduler());

            Action1<Throwable> onError = error -> {
                timeout.unschedule();
                packetHandler.setResponseListener(null);

                responseHandler.onError(error);
            };
            Observable<UUID> subscribe = subscribeResponse(createOperationTimeout("Subscribe"));
            subscribe.subscribe(subscribedCharacteristic -> {
                packetHandler.setResponseListener(new SensePacketHandler.ResponseListener() {
                    @Override
                    public void onDataReady(MorpheusCommand response) {
                        logger.info(GattPeripheral.LOG_TAG, "Got response to command " + command + ": " + response);
                        SensePeripheral.this.commandVersion = response.getVersion();
                        responseHandler.onResponse(response);
                    }

                    @Override
                    public void onError(Throwable error) {
                        timeout.unschedule();

                        if (error instanceof BluetoothConnectionLostError || !isConnected()) {
                            onError.call(error);
                        } else {
                            Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                            logger.error(GattPeripheral.LOG_TAG, "Could not complete command " + command, error);
                            unsubscribe.subscribe(ignored -> onError.call(error), onError);
                        }
                    }
                });

                logger.info(GattPeripheral.LOG_TAG, "Writing command " + command);

                final byte[] commandData = command.toByteArray();
                Observable<Void> write = writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND, commandData);
                write.subscribe(ignored -> {
                    logger.info(GattPeripheral.LOG_TAG, "Wrote command " + command);
                    timeout.schedule();
                }, onError);
            }, onError);
        });
    }

    private Observable<MorpheusCommand> performSimpleCommand(@NonNull MorpheusCommand command,
                                                             @NonNull OperationTimeout commandTimeout) {
        return performCommand(command, commandTimeout, new ResponseHandler<MorpheusCommand>() {
            @Override
            void onResponse(@NonNull MorpheusCommand response) {
                timeout.unschedule();

                Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                unsubscribe.subscribe(ignored -> {
                    packetHandler.setResponseListener(null);

                    if (response.getType() == command.getType()) {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                        propagateResponseError(response, null);
                    } else {
                        propagateUnexpectedResponseError(command.getType(), response);
                    }
                }, e -> {
                    packetHandler.setResponseListener(null);

                    subscriber.onError(e);
                });
            }
        });
    }

    private Observable<MorpheusCommand> performDisconnectingCommand(@NonNull MorpheusCommand command,
                                                                    @NonNull OperationTimeout commandTimeout) {
        return performCommand(command, commandTimeout, new ResponseHandler<MorpheusCommand>() {
            @Override
            void onResponse(@NonNull MorpheusCommand response) {
                timeout.unschedule();

                if (response.getType() == command.getType()) {
                    disconnect().subscribe(ignored -> {
                        packetHandler.setResponseListener(null);

                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    }, e -> {
                        logger.warn(GattPeripheral.LOG_TAG, "Could not cleanly disconnect from Sense, ignoring.", e);

                        packetHandler.setResponseListener(null);

                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    });
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                    packetHandler.setResponseListener(null);

                    propagateResponseError(response, null);
                } else {
                    packetHandler.setResponseListener(null);

                    propagateUnexpectedResponseError(command.getType(), response);
                }
            }

            @Override
            void onError(Throwable e) {
                if (e instanceof BluetoothConnectionLostError) {
                    subscriber.onNext(command);
                    subscriber.onCompleted();
                } else {
                    super.onError(e);
                }
            }
        });
    }

    //endregion

    //region Operations

    @VisibleForTesting
    Observable<Void> writeLargeCommand(@NonNull UUID commandUUID, @NonNull byte[] commandData) {
        List<byte[]> blePackets = packetHandler.createOutgoingPackets(commandData);
        LinkedList<byte[]> remainingPackets = new LinkedList<>(blePackets);

        return Observable.create(subscriber -> {
            Observer<Void> writeObserver = new Observer<Void>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

                @Override
                public void onNext(Void ignored) {
                    remainingPackets.removeFirst();
                    if (remainingPackets.isEmpty()) {
                        logger.info(GattPeripheral.LOG_TAG, "Write large command " + commandUUID);

                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    } else {
                        logger.info(GattPeripheral.LOG_TAG, "Writing next chunk of large command " + commandUUID);
                        gattPeripheral.writeCommand(peripheralService, commandUUID, GattPeripheral.WriteType.NO_RESPONSE, remainingPackets.getFirst(), createOperationTimeout("Write Partial Command")).subscribe(this);
                    }
                }
            };
            logger.info(GattPeripheral.LOG_TAG, "Writing first chunk of large command (" + remainingPackets.size() + " chunks) " + commandUUID);
            gattPeripheral.writeCommand(peripheralService, commandUUID, GattPeripheral.WriteType.NO_RESPONSE, remainingPackets.getFirst(), createOperationTimeout("Write Partial Command")).subscribe(writeObserver);
        });
    }

    public Observable<Void> putIntoNormalMode() {
        logger.info(GattPeripheral.LOG_TAG, "putIntoNormalMode()");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(ignored -> null);
    }

    public Observable<Void> putIntoPairingMode() {
        logger.info(GattPeripheral.LOG_TAG, "putIntoPairingMode()");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();
        return performDisconnectingCommand(morpheusCommand, createSimpleCommandTimeout()).map(ignored -> null);
    }

    public Observable<SenseConnectToWiFiUpdate> connectToWiFiNetwork(@NonNull String ssid,
                                                                     @NonNull wifi_endpoint.sec_type securityType,
                                                                     @Nullable String password) {
        logger.info(GattPeripheral.LOG_TAG, "connectToWiFiNetwork(" + ssid + ")");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        if (securityType != wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN &&
                TextUtils.isEmpty(password)) {
            return Observable.error(new SenseSetWifiValidationError(SenseSetWifiValidationError.Reason.EMPTY_PASSWORD));
        }

        int version = commandVersion;
        MorpheusCommand.Builder builder = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(version)
                .setAppVersion(APP_VERSION)
                .setWifiSSID(ssid)
                .setSecurityType(securityType);
        if (version == COMMAND_VERSION_PVT && securityType == wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WEP) {
            byte[] keyBytes = Bytes.tryFromString(password);
            if (keyBytes == null) {
                return Observable.error(new SenseSetWifiValidationError(SenseSetWifiValidationError.Reason.MALFORMED_BYTES));
            } else if (Bytes.contains(keyBytes, (byte) 0x0)) {
                return Observable.error(new SenseSetWifiValidationError(SenseSetWifiValidationError.Reason.CONTAINS_NUL_BYTE));
            }
            ByteString keyString = ByteString.copyFrom(keyBytes);
            builder.setWifiPasswordBytes(keyString);
        } else {
            builder.setWifiPassword(password);
        }

        MorpheusCommand command = builder.build();
        OperationTimeout commandTimeout = gattPeripheral.createOperationTimeout("Set Wifi", SET_WIFI_TIMEOUT_S, TimeUnit.SECONDS);
        return performCommand(command, commandTimeout, new ResponseHandler<SenseConnectToWiFiUpdate>() {
            @Override
            void onResponse(@NonNull MorpheusCommand response) {
                Action1<Throwable> onError = e -> {
                    packetHandler.setResponseListener(null);
                    subscriber.onError(e);
                };

                if (response.getType() == CommandType.MORPHEUS_COMMAND_CONNECTION_STATE) {
                    timeout.reschedule();

                    SenseConnectToWiFiUpdate status = new SenseConnectToWiFiUpdate(response);
                    logger.info(GattPeripheral.LOG_TAG, "connection state update " + status);

                    if (status.state == wifi_connection_state.CONNECTED) {
                        timeout.unschedule();

                        Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                        unsubscribe.subscribe(ignored -> {
                            packetHandler.setResponseListener(null);

                            subscriber.onNext(status);
                            subscriber.onCompleted();
                        }, onError);
                    } else if (SenseConnectWifiError.isImmediateError(status)) {
                        timeout.unschedule();

                        Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                        unsubscribe.subscribe(ignored -> onError(new SenseConnectWifiError(status, null)),
                                e -> onError(new SenseConnectWifiError(status, e)));

                        packetHandler.setResponseListener(null);
                    } else {
                        subscriber.onNext(status);
                    }
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT) { //old fw
                    timeout.unschedule();

                    Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                    unsubscribe.subscribe(ignored -> {
                        packetHandler.setResponseListener(null);

                        SenseConnectToWiFiUpdate fakeStatus = new SenseConnectToWiFiUpdate(wifi_connection_state.CONNECTED, null, null);
                        subscriber.onNext(fakeStatus);
                        subscriber.onCompleted();
                    }, onError);
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                    timeout.unschedule();

                    Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                    unsubscribe.subscribe(ignored -> propagateResponseError(response, null),
                            e -> propagateResponseError(response, e));

                    packetHandler.setResponseListener(null);
                } else {
                    timeout.unschedule();
                    packetHandler.setResponseListener(null);

                    Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                    unsubscribe.subscribe(ignored -> propagateUnexpectedResponseError(command.getType(), response),
                            e -> propagateUnexpectedResponseError(command.getType(), response));
                }
            }
        });

    }

    public Observable<SenseNetworkStatus> getWifiNetwork() {
        logger.info(GattPeripheral.LOG_TAG, "getWifiNetwork()");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_GET_WIFI_ENDPOINT)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();

        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(response ->
                new SenseNetworkStatus(response.getWifiSSID(), response.getWifiConnectionState()));
    }

    public Observable<String> pairPill(final String accountToken) {
        logger.info(GattPeripheral.LOG_TAG, "pairPill(" + accountToken + ")");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_PILL)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand, createPairPillTimeout()).map(MorpheusCommand::getDeviceId);
    }

    public Observable<Void> linkAccount(final String accountToken) {
        logger.info(GattPeripheral.LOG_TAG, "linkAccount(" + accountToken + ")");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(ignored -> null);
    }

    public Observable<Void> factoryReset() {
        logger.info(GattPeripheral.LOG_TAG, "factoryReset()");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_FACTORY_RESET)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();
        return performDisconnectingCommand(morpheusCommand, createSimpleCommandTimeout()).map(ignored -> null);
    }

    public Observable<Void> pushData() {
        logger.info(GattPeripheral.LOG_TAG, "pushData()");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PUSH_DATA_AFTER_SET_TIMEZONE)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand, createSimpleCommandTimeout()).map(ignored -> null);
    }

    public Observable<Void> runLedAnimation(@NonNull SenseLedAnimation animationType) {
        logger.info(GattPeripheral.LOG_TAG, "runLedAnimation(" + animationType + ")");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(animationType.commandType)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand, createAnimationTimeout()).map(ignored -> null);
    }

    public Observable<List<wifi_endpoint>> scanForWifiNetworks() {
        logger.info(GattPeripheral.LOG_TAG, "scanForWifiNetworks()");

        if (isBusy()) {
            return Observable.error(createBusyError());
        }

        MorpheusCommand command = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_START_WIFISCAN)
                .setVersion(commandVersion)
                .setAppVersion(APP_VERSION)
                .build();
        return performCommand(command, createScanWifiTimeout(), new ResponseHandler<List<wifi_endpoint>>() {
            final List<wifi_endpoint> endpoints = new ArrayList<>();

            @Override
            void onResponse(@NonNull MorpheusCommand response) {
                Action1<Throwable> onError = e -> {
                    packetHandler.setResponseListener(null);
                    subscriber.onError(e);
                };

                if (response.getType() == CommandType.MORPHEUS_COMMAND_START_WIFISCAN) {
                    timeout.reschedule();

                    if (response.getWifiScanResultCount() == 1) {
                        endpoints.add(response.getWifiScanResult(0));
                    }
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_STOP_WIFISCAN) {
                    timeout.unschedule();

                    Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                    unsubscribe.subscribe(ignored -> {
                        packetHandler.setResponseListener(null);

                        subscriber.onNext(endpoints);
                        subscriber.onCompleted();
                    }, onError);
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                    timeout.unschedule();

                    Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                    unsubscribe.subscribe(ignored -> propagateResponseError(response, null),
                                          e -> propagateResponseError(response, e));
                } else {
                    timeout.unschedule();

                    packetHandler.setResponseListener(null);

                    Observable<UUID> unsubscribe = unsubscribeResponse(createOperationTimeout("Unsubscribe"));
                    unsubscribe.subscribe(ignored -> propagateUnexpectedResponseError(command.getType(), response),
                                          e -> propagateUnexpectedResponseError(command.getType(), response));
                }
            }
        });
    }

    //endregion


    private abstract class ResponseHandler<T> {
        Subscriber<? super T> subscriber;
        OperationTimeout timeout;

        void configure(@NonNull Subscriber<? super T> subscriber,
                       @NonNull OperationTimeout timeout) {
            this.subscriber = subscriber;
            this.timeout = timeout;
        }

        abstract void onResponse(@NonNull MorpheusCommand response);

        void propagateResponseError(@NonNull MorpheusCommand response, @Nullable Throwable nestedCause) {
            if (response.getError() == SenseCommandProtos.ErrorType.TIME_OUT) {
                subscriber.onError(new OperationTimeoutError(OperationTimeoutError.Operation.COMMAND_RESPONSE));
            } else {
                subscriber.onError(new SensePeripheralError(response.getError(), nestedCause));
            }
        }

        void propagateUnexpectedResponseError(@NonNull CommandType expected, @NonNull MorpheusCommand response) {
            subscriber.onError(new SenseUnexpectedResponseError(expected, response.getType()));
        }

        void onError(Throwable e) {
            subscriber.onError(e);
        }
    }


    @VisibleForTesting
    public static class Testing {

        @VisibleForTesting
        public static void setPeripheralService(@NonNull SensePeripheral peripheral, PeripheralService service) {
            peripheral.peripheralService = service;
        }
    }
}
