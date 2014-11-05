package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Describes what a BluetoothStack should be looking for when performing a Peripheral discovery operation.
 */
public final class DiscoveryCriteria {
    /**
     * The default timeout duration used by ScanCriteria.
     */
    public static final int DEFAULT_DURATION_MS = 10000;


    /**
     * The addresses the stack should search for.
     * <p/>
     * This list being empty has the effect of the stack accepting any address.
     */
    public final List<String> addresses = new ArrayList<>();

    /**
     * The scan record to match against when searching.
     */
    public byte[] scanRecord;

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
    public static @NonNull DiscoveryCriteria forAddress(@NonNull String address) {
        DiscoveryCriteria criteria = new DiscoveryCriteria();
        criteria.addAddress(address);
        criteria.setLimit(1);
        return criteria;
    }


    //region Builder

    public DiscoveryCriteria addAddress(@NonNull String address) {
        addresses.add(address);
        return this;
    }

    public DiscoveryCriteria setScanRecord(@NonNull byte[] scanRecord) {
        this.scanRecord = scanRecord;
        return this;
    }

    public DiscoveryCriteria setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public DiscoveryCriteria setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public String toString() {
        return "DiscoveryCriteria{" +
                "addresses=" + addresses +
                ", scanRecord=" + Arrays.toString(scanRecord) +
                ", limit=" + limit +
                ", duration=" + duration +
                '}';
    }

    //endregion


    /**
     * Performs a search in a given scan response for the scan record of the criteria.
     */
    public boolean doesScanResponseMatch(@NonNull byte[] scanResponse) {
        if (scanResponse.length < this.scanRecord.length) {
            return false;
        }

        for (int i = 0; i < scanResponse.length - this.scanRecord.length; i++) {
            final byte[] range = Arrays.copyOfRange(scanResponse, i, i + this.scanRecord.length);
            if (Arrays.equals(range, this.scanRecord)) {
                return true;
            }
        }

        return false;
    }
}
