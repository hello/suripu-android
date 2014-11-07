package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;

import rx.Observable;

/**
 * A semi-opaque interface intended to contain all of the necessary logic to interact
 * with a platform Bluetooth stack. Responsible for scanning and vending Peripherals.
 *
 * @see is.hello.sense.bluetooth.stacks.Peripheral
 */
public interface BluetoothStack {
    /**
     * The logging tag that implementations of BluetoothStack should use.
     */
    public static final String LOG_TAG = "Bluetooth." + BluetoothStack.class.getSimpleName();


    /**
     * Performs a scan for peripherals matching a given set of criteria.
     * <p/>
     * Yields {@see is.hello.sense.bluetooth.errors.BluetoothDisabledError}
     * if the device's Bluetooth radio is currently disabled.
     *
     * @see DiscoveryCriteria
     */
    @NonNull Observable<List<Peripheral>> discoverPeripherals(@NonNull DiscoveryCriteria discoveryCriteria);

    /**
     * Vends an observable configured appropriately for use with the BluetoothStack.
     */
    <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe);


    /**
     * Returns a boolean indicating whether or not a given error is
     * fatal in the context of the bluetooth stack implementation,
     * and the client code should disconnect and perform a rediscovery.
     */
    boolean doesErrorRequireReconnect(@Nullable Throwable e);

    /**
     * Returns whether or not the given error is severe enough that
     * it likely cannot be recovered from unless the user restarts
     * all of the wireless radios on their phone.
     */
    boolean isErrorFatal(@Nullable Throwable e);

    /**
     * Returns the behaviors of the stack that are implementation specific.
     */
    EnumSet<Traits> getTraits();

    /**
     * Describes behaviors of the stack that vary between backing implementations.
     */
    public enum Traits {
        /**
         * Indicates that bonding information is wiped out
         * whenever a vended Peripheral disconnects.
         * <p/>
         * Clients should always attempt to bond after connecting.
         *
         * @see Peripheral#createBond()
         */
        BONDS_NOT_PERSISTENT,
    }


    /**
     * Returns the level of support the stack has for the current device.
     */
    SupportLevel getDeviceSupportLevel();

    /**
     * Describes the level of support the current device has in the implementation.
     */
    public enum SupportLevel {
        /**
         * The device is explicitly unsupported, one or more core operations are known to fail.
         */
        UNSUPPORTED,

        /**
         * The device has not been tested, so one or more core operations may not work.
         */
        UNTESTED,

        /**
         * The device is explicitly tested and known to work.
         */
        SUPPORTED,
    }
}
