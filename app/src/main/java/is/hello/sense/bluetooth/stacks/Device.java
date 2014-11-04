package is.hello.sense.bluetooth.stacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import rx.Observable;

public interface Device {
    public static final String LOG_TAG = "Bluetooth/" + Device.class.getSimpleName();

    public static final int BOND_NONE = BluetoothDevice.BOND_NONE;
    public static final int BOND_BONDING = BluetoothDevice.BOND_BONDING;
    public static final int BOND_BONDED = BluetoothDevice.BOND_BONDED;

    public static final int STATUS_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public static final int STATUS_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    public static final int STATUS_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public static final int STATUS_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    //region Properties

    int getScannedRssi();
    String getAddress();
    String getName();
    DeviceCenter getDeviceCenter();

    //endregion


    //region Connectivity

    @NonNull Observable<Device> connect();
    @NonNull Observable<Device> disconnect();
    int getConnectionStatus();

    //endregion


    //region Bonding

    @NonNull Observable<Device> bond();
    @NonNull Observable<Device> unbond();
    int getBondStatus();

    //endregion


    //region Discovering Services

    @NonNull Observable<List<Service>> discoverServices();
    @Nullable Service getService(@NonNull UUID serviceIdentifier);

    //endregion


    //region Characteristics

    @NonNull Observable<UUID> subscribeNotification(@NonNull Service onService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier);
    @NonNull Observable<UUID> unsubscribeNotification(@NonNull Service onService,
                                                      @NonNull UUID characteristicIdentifier,
                                                      @NonNull UUID descriptorIdentifier);

    @NonNull Observable<Void> writeCommand(@NonNull Service onService, @NonNull Command command);

    void setPacketHandler(@Nullable PacketHandler dataHandler);
    @Nullable PacketHandler getPacketHandler();

    //endregion
}
