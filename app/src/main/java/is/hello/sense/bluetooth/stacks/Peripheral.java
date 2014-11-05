package is.hello.sense.bluetooth.stacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import rx.Observable;

/**
 * Represents a Bluetooth Low Energy device that communicates over a gatt profile.
 * <p/>
 * All Observable objects returned by a Peripheral must be subscribed
 * to before they will perform their work. No guarantees are made about
 * what scheduler the Observables will do, and yield their work on.
 */
public interface Peripheral {
    /**
     * The logging tag that should be used by implementations of the Peripheral interface.
     */
    public static final String LOG_TAG = "Bluetooth." + Peripheral.class.getSimpleName();


    //region Bond Status

    /**
     * Indicates the peripheral is not bonded.
     */
    public static final int BOND_NONE = BluetoothDevice.BOND_NONE;

    /**
     * Indicates the peripheral is in the process of being bonded.
     */
    public static final int BOND_BONDING = BluetoothDevice.BOND_BONDING;

    /**
     * Indicates the peripheral is bonded.
     */
    public static final int BOND_BONDED = BluetoothDevice.BOND_BONDED;

    //endregion


    //region Connection Status

    /**
     * Indicates the Peripheral is not connected.
     */
    public static final int STATUS_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;

    /**
     * Indicates the Peripheral is in the process of connecting.
     */
    public static final int STATUS_CONNECTING = BluetoothProfile.STATE_CONNECTING;

    /**
     * Indicates the Peripheral is connected.
     */
    public static final int STATUS_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    /**
     * Indicates the Peripheral is in the process of disconnecting.
     */
    public static final int STATUS_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    //endregion


    //region Properties

    /**
     * Returns the received signal strength of the Peripheral
     * when it was discovered by the {@see BluetoothStack}.
     * <p/>
     * This value does not update.
     */
    int getScanTimeRssi();

    /**
     * Returns the address of the Peripheral.
     * <p/>
     * This value should be included in the implementation's toString method.
     */
    String getAddress();

    /**
     * Returns the name of the Peripheral.
     * <p/>
     * This value should be included in the implementation's toString method.
     */
    String getName();

    /**
     * Returns the stack this Peripheral is tied to.
     * <p/>
     * @see is.hello.sense.bluetooth.stacks.BluetoothStack#newConfiguredObservable(rx.Observable.OnSubscribe)
     */
    BluetoothStack getStack();

    //endregion


    //region Connectivity

    /**
     * Attempts to create a gatt connection to the peripheral.
     * <p/>
     * Does nothing if there is already an active connection.
     * <p/>
     * Yields an {@see is.hello.sense.bluetooth.errors.PeripheralConnectionError} if called
     * when peripheral connection status is changing.
     */
    @NonNull Observable<Peripheral> connect();

    /**
     * Ends the gatt connection of the peripheral.
     * <p/>
     * Yields {@see is.hello.sense.bluetooth.errors.NotConnectedException}
     * if the peripheral is not connected.
     */
    @NonNull Observable<Peripheral> disconnect();

    /**
     * Returns the connection status of the Peripheral.
     *
     * @see Peripheral#STATUS_DISCONNECTED
     * @see Peripheral#STATUS_CONNECTING
     * @see Peripheral#STATUS_CONNECTED
     * @see Peripheral#STATUS_DISCONNECTING
     */
    int getConnectionStatus();

    //endregion


    //region Bonding

    /**
     * Creates a bond to the peripheral from the current device.
     * <p/>
     * Does nothing if the device is already bonded.
     */
    @NonNull Observable<Peripheral> createBond();

    /**
     * Removes the bond to the peripheral from the current device.
     * <p/>
     * Does nothing if the device is not bonded.
     */
    @NonNull Observable<Peripheral> removeBond();

    /**
     * Returns the bond status of the peripheral.
     *
     * @see Peripheral#BOND_NONE
     * @see Peripheral#BOND_BONDING
     * @see Peripheral#BOND_BONDED
     */
    int getBondStatus();

    //endregion


    //region Discovering Services

    /**
     * Performs service discovery on the peripheral.
     * <p/>
     * Yields a {@see NotConnectedException} if the peripheral
     * is not connected when this method is called.
     *
     * @see Peripheral#getService(java.util.UUID)
     */
    @NonNull Observable<Collection<PeripheralService>> discoverServices(@NonNull OperationTimeout timeout);

    /**
     * Looks up a peripheral service by identifier on the peripheral.
     * <p/>
     * This method requires {@see Peripheral#discoverServices()}
     * be called before it will return a non-null value.
     */
    @Nullable PeripheralService getService(@NonNull UUID serviceIdentifier);

    //endregion


    //region Characteristics

    @NonNull Observable<UUID> subscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier,
                                                    @NonNull OperationTimeout timeout);
    @NonNull Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                      @NonNull UUID characteristicIdentifier,
                                                      @NonNull UUID descriptorIdentifier,
                                                      @NonNull OperationTimeout timeout);

    @NonNull Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService,
                                           @NonNull UUID identifier,
                                           @NonNull byte[] payload,
                                           @NonNull OperationTimeout timeout);

    /**
     * Associates a given packet handler with the Peripheral.
     * <p/>
     * All characteristic data read by the Peripheral should be piped into the packet handler.
     */
    void setPacketHandler(@Nullable PacketHandler dataHandler);

    /**
     * Returns the associated packet handler of the Peripheral.
     */
    @Nullable PacketHandler getPacketHandler();

    //endregion
}
