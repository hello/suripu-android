package is.hello.sense.bluetooth.stacks.util;

import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import rx.functions.Func1;

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
     * A list of predicate functions that will be called to determine
     * whether or not a collection of advertising data matches this
     * scan criteria object. <em>All predicates must evaluate to true
     * for the scan criteria to match a potential peripheral.</em>
     */
    public final List<Func1<AdvertisingData, Boolean>> predicates = new ArrayList<>();

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


    public ScanCriteria addPredicate(@NonNull Func1<AdvertisingData, Boolean> predicate) {
        predicates.add(predicate);
        return this;
    }

    public ScanCriteria addExactMatchPredicate(int type, @NonNull byte[] toMatch) {
        return addPredicate(ad -> ad.anyMatches(type, match -> Arrays.equals(match, toMatch)));
    }

    public ScanCriteria addExactMatchPredicate(int type, @NonNull String toMatch) {
        return addExactMatchPredicate(type, BluetoothUtils.stringToBytes(toMatch));
    }

    public ScanCriteria addStartsWithPredicate(int type, @NonNull byte[] prefix) {
        return addPredicate(ad -> ad.anyMatches(type, match -> BluetoothUtils.bytesStartsWith(match, prefix)));
    }

    public ScanCriteria addStartsWithPredicate(int type, @NonNull String prefix) {
        return addStartsWithPredicate(type, BluetoothUtils.stringToBytes(prefix));
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

    public boolean matches(@NonNull AdvertisingData scanResponses) {
        for (Func1<AdvertisingData, Boolean> predicate : predicates) {
            if (!predicate.call(scanResponses)) {
                return false;
            }
        }

        return true;
    }
}
