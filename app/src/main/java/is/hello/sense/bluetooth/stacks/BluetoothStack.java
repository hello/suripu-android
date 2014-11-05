package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

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
     * @see DiscoveryCriteria
     */
    @NonNull Observable<List<Peripheral>> discoverPeripherals(@NonNull DiscoveryCriteria discoveryCriteria);

    /**
     * Vends an observable configured appropriately for use with the BluetoothStack.
     */
    <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe);
}
