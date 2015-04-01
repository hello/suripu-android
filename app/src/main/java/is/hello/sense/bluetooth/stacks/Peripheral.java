package is.hello.sense.bluetooth.stacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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


    //region Local Broadcasts

    /**
     * A local broadcast that informs interested listeners that a Peripheral has disconnected.
     *
     * @see #EXTRA_NAME
     * @see #EXTRA_ADDRESS
     */
    public static final String ACTION_DISCONNECTED = Peripheral.class.getName() + ".ACTION_DISCONNECTED";

    /**
     * The name of the affected Peripheral.
     *
     * @see #ACTION_DISCONNECTED
     */
    public static final String EXTRA_NAME = Peripheral.class.getName() + ".EXTRA_NAME";

    /**
     * The address of the affected Peripheral.
     *
     * @see #ACTION_DISCONNECTED
     */
    public static final String EXTRA_ADDRESS = Peripheral.class.getName() + ".EXTRA_ADDRESS";

    //endregion


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

    @IntDef({BOND_NONE, BOND_BONDING, BOND_BONDED})
    @Retention(RetentionPolicy.SOURCE)
    @interface BondStatus {}

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

    @IntDef({STATUS_DISCONNECTED, STATUS_DISCONNECTING, STATUS_CONNECTED, STATUS_CONNECTING})
    @Retention(RetentionPolicy.SOURCE)
    @interface ConnectivityStatus {}

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


    //region Timeouts

    /**
     * Returns a new operation timeout for use with the Peripheral.
     */
    @NonNull OperationTimeout createOperationTimeout(@NonNull String name, long duration, @NonNull TimeUnit timeUnit);

    //endregion


    //region Connectivity

    /**
     * Attempts to create a gatt connection to the peripheral.
     * <p/>
     * Does nothing if there is already an active connection.
     * <p/>
     * Yields an {@link is.hello.sense.bluetooth.errors.PeripheralConnectionError} if called
     * when peripheral connection status is changing.
     * @param timeout   The timeout to apply to the connect operation. Will only fire on certain phones.
     */
    @NonNull Observable<Peripheral> connect(@NonNull OperationTimeout timeout);

    /**
     * Ends the gatt connection of the peripheral.
     * <p/>
     * Safe to call multiple times if the peripheral is disconnected,
     * or in the process of disconnecting.
     * <p/>
     * Yields a {@link is.hello.sense.bluetooth.errors.PeripheralConnectionError}
     * if the peripheral is currently connecting.
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
    @ConnectivityStatus int getConnectionStatus();

    //endregion


    //region Bonding

    /**
     * Creates a bond to the peripheral from the current device.
     * <p/>
     * Does nothing if the device is already bonded.
     */
    @NonNull Observable<Peripheral> createBond();

    /**
     * Returns the bond status of the peripheral.
     *
     * @see Peripheral#BOND_NONE
     * @see Peripheral#BOND_BONDING
     * @see Peripheral#BOND_BONDED
     */
    @BondStatus int getBondStatus();

    //endregion


    //region Discovering Services

    /**
     * Performs service discovery on the peripheral.
     * <p/>
     * Yields a {@link is.hello.sense.bluetooth.errors.PeripheralConnectionError}
     * if the peripheral is not connected when this method is called.
     *
     * @see Peripheral#getService(java.util.UUID)
     */
    @NonNull Observable<Collection<PeripheralService>> discoverServices(@NonNull OperationTimeout timeout);

    /**
     * Performs service discovery on the peripheral,
     * yielding the service matching a given identifier.
     * <p/>
     * If the service cannot be found, this method will yield
     * a {@link is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError}.
     * <p/>
     * Yields a {@link is.hello.sense.bluetooth.errors.PeripheralConnectionError}
     * if the peripheral is not connected when this method is called.
     *
     * @see #discoverServices(OperationTimeout)
     * @see #getService(java.util.UUID)
     */
    @NonNull Observable<PeripheralService> discoverService(@NonNull UUID serviceIdentifier, @NonNull OperationTimeout timeout);

    /**
     * Returns whether or not the Peripheral has performed service discovery.
     */
    boolean hasDiscoveredServices();

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

    //endregion


    //region Configuration

    /**
     * Represents no config options specified.
     */
    int CONFIG_EMPTY = 0;

    /**
     * Whether or not the peripheral should clear bond information
     * before connecting to a hardware device.
     */
    int CONFIG_FRAGILE_BONDS = (1 << 1);

    /**
     * Whether or not the peripheral should automatically
     * activate compatibility shims in response to errors.
     */
    int CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS = (1 << 2);

    /**
     * Whether or not to add an artificial delay after
     * service discovery to increase connection stability.
     */
    int CONFIG_WAIT_AFTER_SERVICE_DISCOVERY = (1 << 3);

    void setConfig(@Config int newConfig);

    @IntDef(value = {
        CONFIG_EMPTY,
        CONFIG_FRAGILE_BONDS,
        CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS,
        CONFIG_WAIT_AFTER_SERVICE_DISCOVERY
    }, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    @interface Config {}

    //endregion
}
