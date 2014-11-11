package is.hello.sense.bluetooth.stacks.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Describes what a BluetoothStack should be looking for when performing a Peripheral discovery operation.
 */
public final class ScanCriteria {
    /**
     * The default timeout duration used by ScanCriteria.
     */
    public static final int DEFAULT_DURATION_MS = 10000;

    /**
     * Device addresses to match against when performing a scan.
     * <p/>
     * This list being empty has the effect of all addresses being acceptable.
     */
    public final List<String> peripheralAddresses = new ArrayList<>();

    /**
     * Advertising data to match against when a device is discovered.
     * <p/>
     * This list being empty has the effect of advertising data
     * not being used to determine what devices are acceptable.
     */
    public final List<ScanResponse> constraints = new ArrayList<>();

    /**
     * The maximum number of peripherals to scan before stopping.
     */
    public int limit = Integer.MAX_VALUE;

    /**
     * The maximum amount of time that can pass before the scan is ended.
     */
    public long duration = DEFAULT_DURATION_MS;


    /**
     * Returns a configured criteria that will search for one instance of a given address.
     */
    public static @NonNull ScanCriteria forAddress(@NonNull String address) {
        ScanCriteria criteria = new ScanCriteria();
        criteria.addPeripheralAddress(address);
        criteria.setLimit(1);
        return criteria;
    }


    //region Builder

    public ScanCriteria addPeripheralAddress(@NonNull String address) {
        peripheralAddresses.add(address);
        return this;
    }

    public ScanCriteria addConstraint(@NonNull ScanResponse scanResponse) {
        constraints.add(scanResponse);
        return this;
    }

    public ScanCriteria setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public ScanCriteria setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    //endregion

    public boolean matches(@NonNull Set<ScanResponse> scanResponses) {
        return scanResponses.isEmpty() || scanResponses.containsAll(constraints);
    }
}
