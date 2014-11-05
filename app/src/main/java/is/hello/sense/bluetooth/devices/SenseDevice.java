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
import is.hello.sense.bluetooth.stacks.Command;
import is.hello.sense.bluetooth.stacks.Device;
import is.hello.sense.bluetooth.stacks.DeviceCenter;
import is.hello.sense.bluetooth.stacks.Service;
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

public class SenseDevice {
    private static int COMMAND_VERSION = 0;

    private static final byte[] MORPHEUS_SERVICE_UUID_BYTES = new byte[]{
            0x23, (byte) 0xD1, (byte) 0xBC, (byte) 0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte) 0xEF,   // EFDE
            0x12, 0x12,   // 1212
            (byte) 0xE1, (byte) 0xFE, 0x00, 0x00  // 0000FEE1
    };

    private final Device device;
    private Service service;
    private PacketDataHandler<MorpheusCommand> dataHandler;
    private PacketHandler packetHandler;

    public static Observable<List<SenseDevice>> scan(@NonNull DeviceCenter deviceCenter, @NonNull DeviceCenter.ScanCriteria criteria) {
        criteria.setScanRecord(MORPHEUS_SERVICE_UUID_BYTES);
        return deviceCenter.scanForDevice(criteria, 10 * 1000).map(SenseDevice::fromDevices);
    }

    public static List<SenseDevice> fromDevices(@NonNull List<Device> devices) {
        List<SenseDevice> mapped = new ArrayList<>();
        for (Device device : devices) {
            mapped.add(new SenseDevice(device));
        }
        return mapped;
    }

    public SenseDevice(@NonNull Device device) {
        this.device = device;
        this.dataHandler = new SensePacketDataHandler();
        this.packetHandler = new SensePacketHandler();
        packetHandler.registerDataHandler(dataHandler);
        device.setPacketHandler(packetHandler);
    }


    public Device getDevice() {
        return device;
    }

    public int getScannedRssi() {
        return device.getScannedRssi();
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return device.getName();
    }


    //region Connectivity

    public Observable<SenseDevice> connect() {
        Logger.info(Device.LOG_TAG, "connect to " + toString());

        return Observable.create((Observable.OnSubscribe<SenseDevice>) s -> {
            device.connect().subscribe(device -> {
                Logger.info(Device.LOG_TAG, "connected to " + toString());
                device.bond().subscribe(ignored -> {
                    Logger.info(Device.LOG_TAG, "bonded to " + toString());
                    device.discoverServices().subscribe(services -> {
                        Logger.info(Device.LOG_TAG, "discovered services for " + toString());
                        this.service = device.getService(SenseIdentifiers.SENSE_SERVICE_UUID);
                        s.onNext(this);
                        s.onCompleted();
                    }, s::onError);
                }, s::onError);
            }, s::onError);
        });
    }

    public Observable<SenseDevice> disconnect() {
        return device.disconnect().map(ignored -> this);
    }

    public boolean isConnected() {
        return device.getConnectionStatus() == Device.STATUS_CONNECTED;
    }

    public int getBondStatus() {
        return device.getBondStatus();
    }

    //endregion


    //region Internal

    protected Service getTargetService() {
        return service;
    }

    protected UUID getDescriptorIdentifier() {
        return SenseIdentifiers.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG;
    }

    protected Observable<UUID> subscribe(@NonNull UUID characteristicIdentifier) {
        Logger.info(Device.LOG_TAG, "Subscribing to " + characteristicIdentifier);

        return device.subscribeNotification(getTargetService(), characteristicIdentifier, getDescriptorIdentifier());
    }

    protected Observable<UUID> unsubscribe(@NonNull UUID characteristicIdentifier) {
        Logger.info(Device.LOG_TAG, "Unsubscribing from " + characteristicIdentifier);

        return device.unsubscribeNotification(getTargetService(), characteristicIdentifier, getDescriptorIdentifier());
    }

    protected Observable<MorpheusCommand> performCommand(@NonNull MorpheusCommand command,
                                                         @NonNull Action2<MorpheusCommand, Subscriber<? super MorpheusCommand>> onFinish) {
        return device.getDeviceCenter().newConfiguredObservable(s -> {
            Action1<Throwable> onError = s::onError;
            Observable<UUID> subscription = subscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID);
            subscription.subscribe(subscribedCharacteristic -> {
                dataHandler.onResponse = response -> {
                    Logger.info(Device.LOG_TAG, "Got response to command " + command + ": " + response);
                    onFinish.call(response, s);
                };
                dataHandler.onError = dataError -> {
                    Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID);
                    Logger.error(Device.LOG_TAG, "Could not complete command " + command, dataError);
                    unsubscription.subscribe(ignored -> s.onError(dataError), onError);
                };

                Logger.info(Device.LOG_TAG, "Writing command " + command);
                final byte[] commandData = command.toByteArray();
                Observable<Void> write = writeLargeCommand(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_UUID, commandData);
                write.subscribe(ignored -> Logger.info(Device.LOG_TAG, "Wrote command " + command), onError);
            }, onError);
        });
    }

    protected Observable<MorpheusCommand> performSimpleCommand(@NonNull MorpheusCommand command) {
        return performCommand(command, (response, s) -> {
            Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID);
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
                        Logger.info(Device.LOG_TAG, "Write large command " + commandUUID);

                        s.onNext(null);
                        s.onCompleted();
                    } else {
                        Logger.info(Device.LOG_TAG, "Writing next chunk of large command " + commandUUID);

                        Command command = Command.with(commandUUID, remainingPackets.getFirst());
                        device.writeCommand(service, command).subscribe(this);
                    }
                }
            };
            Logger.info(Device.LOG_TAG, "Writing first chunk of large command (" + remainingPackets.size() + " chunks) " + commandUUID);
            Command firstCommand = Command.with(commandUUID, remainingPackets.getFirst());
            device.writeCommand(service, firstCommand).subscribe(writeObserver);
        });
    }

    public Observable<Void> setPairingModeEnabled(boolean enabled) {
        Logger.info(Device.LOG_TAG, "setPairingModeEnabled(" + enabled + ")");

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
        Logger.info(Device.LOG_TAG, "clearPairedUser()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_EREASE_PAIRED_PHONE)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> wipeFirmware() {
        Logger.info(Device.LOG_TAG, "wipeFirmware()");

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
        Logger.info(Device.LOG_TAG, "setWifiNetwork(" + ssid + ")");

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
        Logger.info(Device.LOG_TAG, "pairPill(" + accountToken + ")");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(COMMAND_VERSION)
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand).map(MorpheusCommand::getDeviceId);
    }

    public Observable<Void> linkAccount(final String accountToken) {
        Logger.info(Device.LOG_TAG, "linkAccount(" + accountToken + ")");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(COMMAND_VERSION)
                .setAccountId(accountToken)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<Void> factoryReset() {
        Logger.info(Device.LOG_TAG, "factoryReset()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_FACTORY_RESET)
                .setVersion(COMMAND_VERSION)
                .build();
        return performSimpleCommand(morpheusCommand).map(ignored -> null);
    }

    public Observable<List<SenseBle.wifi_endpoint>> scanForWifiNetworks() {
        Logger.info(Device.LOG_TAG, "scanForWifiNetworks()");

        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(CommandType.MORPHEUS_COMMAND_START_WIFISCAN)
                .setVersion(COMMAND_VERSION)
                .build();

        //noinspection MismatchedQueryAndUpdateOfCollection
        List<SenseBle.wifi_endpoint> endpoints = new ArrayList<>();
        Observable<UUID> unsubscription = unsubscribe(SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID);
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
        Logger.info(Device.LOG_TAG, "deviceId()");

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
                ", connectionStatus=" + device.getConnectionStatus() +
                ", bondStatus=" + device.getBondStatus() +
                ", scannedRssi=" + getScannedRssi() +
                '}';
    }
}
