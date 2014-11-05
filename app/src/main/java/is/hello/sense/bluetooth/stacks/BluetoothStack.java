package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

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
     * Returns the behaviors of the stack that are implementation specific.
     */
    EnumSet<Traits> getTraits();

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
}
