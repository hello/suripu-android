package is.hello.sense.bluetooth.devices;

import android.support.annotation.NonNull;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.protobuf.MorpheusBle;
import com.hello.ble.stack.application.MorpheusProtobufResponseDataHandler;
import com.hello.ble.stack.transmission.MorpheusBlePacketHandler;
import com.hello.ble.util.BleUUID;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.Command;
import is.hello.sense.bluetooth.stacks.Device;
import is.hello.sense.bluetooth.stacks.DeviceCenter;
import is.hello.sense.bluetooth.stacks.Service;
import is.hello.sense.util.BleObserverCallback;
import rx.Observable;
import rx.Observer;

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
    private MorpheusProtobufResponseDataHandler dataHandler;
    private MorpheusBlePacketHandler packetHandler;

    public static Observable<List<SenseDevice>> scan(@NonNull DeviceCenter deviceCenter) {
        DeviceCenter.ScanCriteria criteria = new DeviceCenter.ScanCriteria();
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
        this.dataHandler = new MorpheusProtobufResponseDataHandler(null);
        this.packetHandler = new MorpheusBlePacketHandler();
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
        return Observable.create((Observable.OnSubscribe<SenseDevice>) s -> {
            device.connect().subscribe(device -> {
                device.bond().subscribe(ignored -> {
                    device.discoverServices().subscribe(services -> {
                        this.service = device.getService(BleUUID.MORPHEUS_SERVICE_UUID);
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

    //endregion


    //region Operations

    private Observable<Void> writeLargeCommand(UUID commandUUID, byte[] commandData) {
        List<byte[]> blePackets = packetHandler.prepareBlePacket(commandData);
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
                        s.onNext(null);
                        s.onCompleted();
                    } else {
                        Command command = Command.with(commandUUID, remainingPackets.getFirst());
                        device.writeCommand(service, command).subscribe(this);
                    }
                }
            };
            Command firstCommand = Command.with(commandUUID, remainingPackets.getFirst());
            device.writeCommand(service, firstCommand).subscribe(writeObserver);
        });
    }

    public Observable<String> deviceId() {
        return Observable.create((Observable.OnSubscribe<String>) s -> {
            dataHandler.setDataCallback(new BleOperationCallback<MorpheusBle.MorpheusCommand>() {
                @Override
                public void onCompleted(HelloBleDevice sender, MorpheusBle.MorpheusCommand data) {
                    Observable<UUID> unsubscribe = device.unsubscribeNotification(service, BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, BleUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
                    unsubscribe.subscribe(ignored -> {
                        s.onNext(data.getDeviceId());
                        s.onCompleted();
                    }, s::onError);
                }

                @Override
                public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                    Observable<UUID> unsubscribe = device.unsubscribeNotification(service, BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, BleUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
                    unsubscribe.subscribe(ignored -> s.onError(new BleObserverCallback.BluetoothError(reason, errorCode)), s::onError);
                }
            });

            Observable<UUID> subscribe = device.subscribeNotification(service, BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, BleUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
            subscribe.subscribe(ignored -> {
                final MorpheusBle.MorpheusCommand morpheusCommand = MorpheusBle.MorpheusCommand.newBuilder()
                        .setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_GET_DEVICE_ID)
                        .setVersion(COMMAND_VERSION)
                        .build();
                final byte[] commandData = morpheusCommand.toByteArray();
                Observable<Void> write = writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, commandData);
                write.subscribe(ignored1 -> {}, s::onError);
            }, s::onError);
        });
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
