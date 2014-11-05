package is.hello.sense.bluetooth.stacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import rx.Observable;

public interface Peripheral {
    public static final String LOG_TAG = "Bluetooth." + Peripheral.class.getSimpleName();

    public static final int BOND_NONE = BluetoothDevice.BOND_NONE;
    public static final int BOND_BONDING = BluetoothDevice.BOND_BONDING;
    public static final int BOND_BONDED = BluetoothDevice.BOND_BONDED;

    public static final int STATUS_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public static final int STATUS_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    public static final int STATUS_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public static final int STATUS_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    //region Properties

    int getScanTimeRssi();
    String getAddress();
    String getName();
    BluetoothStack getStack();

    //endregion


    //region Connectivity

    @NonNull Observable<Peripheral> connect();
    @NonNull Observable<Peripheral> disconnect();
    int getConnectionStatus();

    //endregion


    //region Bonding

    @NonNull Observable<Peripheral> bond();
    @NonNull Observable<Peripheral> unbond();
    int getBondStatus();

    //endregion


    //region Discovering Services

    @NonNull Observable<List<PeripheralService>> discoverServices();
    @Nullable PeripheralService getService(@NonNull UUID serviceIdentifier);

    //endregion


    //region Characteristics

    @NonNull Observable<UUID> subscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier);
    @NonNull Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                      @NonNull UUID characteristicIdentifier,
                                                      @NonNull UUID descriptorIdentifier);

    @NonNull Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService, @NonNull Command command);

    void setPacketHandler(@Nullable PacketHandler dataHandler);
    @Nullable PacketHandler getPacketHandler();

    //endregion
}
