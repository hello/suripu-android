package is.hello.sense.bluetooth.devices;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.transmission.SensePacketDataHandler;
import is.hello.sense.bluetooth.devices.transmission.SensePacketHandler;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseBle;
import is.hello.sense.bluetooth.errors.GattException;
import is.hello.sense.bluetooth.errors.SenseException;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Command;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.ScanCriteria;
import is.hello.sense.bluetooth.stacks.transmission.PacketDataHandler;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Action2;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseBle.MorpheusCommand;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseBle.MorpheusCommand.CommandType;

public class SensePeripheral {
    private static int COMMAND_VERSION = 0;

    private final Peripheral peripheral;
    private PeripheralService peripheralService;
    private PacketDataHandler<MorpheusCommand> dataHandler;
    private PacketHandler packetHandler;

    public static Observable<List<SensePeripheral>> discover(@NonNull BluetoothStack bluetoothStack,
                                                             @NonNull ScanCriteria criteria) {
        criteria.setScanRecord(SenseIdentifiers.SENSE_SERVICE_BYTES);
        return bluetoothStack.scanForDevice(criteria)
                             .map(SensePeripheral::fromDevices);
    }

    public static List<SensePeripheral> fromDevices(@NonNull List<Peripheral> peripherals) {
        List<SensePeripheral> mapped = new ArrayList<>();
        for (Peripheral peripheral : peripherals) {
            mapped.add(new SensePeripheral(peripheral));
        }
        return mapped;
    }

    public SensePeripheral(@NonNull Peripheral peripheral) {
        this.peripheral = peripheral;
        this.dataHandler = new SensePacketDataHandler();
        this.packetHandler = new SensePacketHandler();
        packetHandler.setPacketDataHandler(dataHandler);
        peripheral.setPacketHandler(packetHandler);
    }



    public int getScannedRssi() {
        return peripheral.getScanTimeRssi();
    }

    public String getAddress() {
        return peripheral.getAddress();
    }

    public String getName() {
        return peripheral.getName();
    }


    //region Connectivity

    public Observable<SensePeripheral> connect() {
        Logger.info(Peripheral.LOG_TAG, "connect to " + toString());

        return peripheral.getStack().newConfiguredObservable(s -> {
            peripheral.connect().subscribe(device -> {
                Logger.info(Peripheral.LOG_TAG, "connected to " + toString());
                device.bond().subscribe(ignored -> {
                    Logger.info(Peripheral.LOG_TAG, "bonded to " + toString());
                    device.discoverServices().subscribe(services -> {
                        Logger.info(Peripheral.LOG_TAG, "discovered services for " + toString());
                        this.peripheralService = device.getService(SenseIdentifiers.SENSE_SERVICE);
                        s.onNext(this);
                        s.onCompleted();
                    }, s::onError);
                }, s::onError);
            }, s::onError);
        });
    }

    public Observable<SensePeripheral> disconnect() {
        return peripheral.disconnect().map(ignored -> this);
    }

    public boolean isConnected() {
        return peripheral.getConnectionStatus() == Peripheral.STATUS_CONNECTED;
    }

    public int getBondStatus() {
        return peripheral.getBondStatus();
    }

    //endregion


    //region Internal

    protected PeripheralService getTargetService() {
        return peripheralService;
    }

    protected UUID getDescriptorIdentifier() {
        return SenseIdentifiers.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG;
    }

    protected Observable<UUID> subscribe(@NonNull UUID characteristicIdentifier) {
        Logger.info(Peripheral.LOG_TAG, "Subscribing to " + characteristicIdentifier);

        return peripheral.subscribeNotification(getTargetService(), characteristicIdentifier, getDescriptorIdentifier());
    }

    protected Observable<UUID> unsubscribe(@NonNull UUID characteristicIdentifier) {
        Logger.info(Peripheral.LOG_TAG, "Unsubscribing from " + characteristicIdentifier);

        return peripheral.unsubscribeNotification(getTargetService(), characteristicIdentifier, getDescriptorIdentifier());
    }

    protected Observable<MorpheusCommand> performCommand(@NonNull MorpheusCommand command,
                                                         @NonNull Action2<MorpheusCommand, Subscriber<? super MorpheusCommand>> onFinish) {
        return peripheral.getStack().newConfiguredObservable(s -> {
            Action1<Throwable> onError = s::onError;
            Observable<UUID> subscription = subscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
            subscription.subscribe(subscribedCharacteristic -> {
                dataHandler.onResponse = response -> {
                    Logger.info(Peripheral.LOG_TAG, "Got response to command " + command + ": " + response);
                    onFinish.call(response, s);
                };
                dataHandler.onError = dataError -> {
                    Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
                    Logger.error(Peripheral.LOG_TAG, "Could not complete command " + command, dataError);
                    unsubscription.subscribe(ignored -> s.onError(dataError), onError);
                };

                Logger.info(Peripheral.LOG_TAG, "Writing command " + command);
                final byte[] commandData = command.toByteArray();
                Observable<Void> write = writeLargeCommand(SenseIdentifiers.CHAR_PROTOBUF_COMMAND, commandData);
                write.subscribe(ignored -> Logger.info(Peripheral.LOG_TAG, "Wrote command " + command), onError);
            }, onError);
        });
    }

    protected Observable<MorpheusCommand> performSimpleCommand(@NonNull MorpheusCommand command) {
        return performCommand(command, (response, s) -> {
            Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
            unsubscription.subscribe(ignored -> {
                if (response.getType() == command.getType()) {
                    s.onNext(response);
                    s.onCompleted();
                } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                    s.onError(new SenseException(command.getError()));
                } else {
                    s.onError(new GattException(0));
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

                        Command command = Command.with(commandUUID, remainingPackets.getFirst());
                        peripheral.writeCommand(peripheralService, command).subscribe(this);
                    }
                }
            };
            Logger.info(Peripheral.LOG_TAG, "Writing first chunk of large command (" + remainingPackets.size() + " chunks) " + commandUUID);
            Command firstCommand = Command.with(commandUUID, remainingPackets.getFirst());
            peripheral.writeCommand(peripheralService, firstCommand).subscribe(writeObserver);
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

    public Observable<Void> clearPairedUser() {
        Logger.info(Peripheral.LOG_TAG, "clearPairedUser()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_EREASE_PAIRED_PHONE)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> wipeFirmware() {
        Logger.info(Peripheral.LOG_TAG, "wipeFirmware()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_MORPHEUS_DFU_BEGIN)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> setWifiNetwork(String bssid,
                                           String ssid,
                                           SenseBle.wifi_endpoint.sec_type securityType,
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
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
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

    public Observable<List<SenseBle.wifi_endpoint>> scanForWifiNetworks() {
        Logger.info(Peripheral.LOG_TAG, "scanForWifiNetworks()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_START_WIFISCAN)
                .setVersion(COMMAND_VERSION)
                .build();

        //noinspection MismatchedQueryAndUpdateOfCollection
        List<SenseBle.wifi_endpoint> endpoints = new ArrayList<>();
        Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE);
        return performCommand(morpheusCommand, (response, subscriber) -> {
            if (response.getType() == CommandType.MORPHEUS_COMMAND_START_WIFISCAN) {
                if(response.getWifiScanResultCount() == 1) {
                    endpoints.add(response.getWifiScanResult(0));
                }
            } else if (response.getType() == CommandType.MORPHEUS_COMMAND_STOP_WIFISCAN) {
                unsubscription.subscribe(ignored -> {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                }, subscriber::onError);
            } else if (response.getType() == CommandType.MORPHEUS_COMMAND_ERROR) {
                unsubscription.subscribe(ignored -> subscriber.onError(new SenseException(response.getError())), subscriber::onError);
            } else {
                unsubscription.subscribe(ignored -> subscriber.onError(new GattException(0)), subscriber::onError);
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


    @Override
    public String toString() {
        return "{SenseDevice " +
                "name=" + getName() +
                ", address=" + getAddress() +
                ", connectionStatus=" + peripheral.getConnectionStatus() +
                ", bondStatus=" + peripheral.getBondStatus() +
                ", scannedRssi=" + getScannedRssi() +
                '}';
    }
}
