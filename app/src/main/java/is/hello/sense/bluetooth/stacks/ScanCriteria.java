package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ScanCriteria {
    public static final int DEFAULT_DURATION_MS = 10000;

    public List<String> addresses = new ArrayList<>();
    public byte[] scanRecord;
    public int limit = Integer.MAX_VALUE;
    public long duration = DEFAULT_DURATION_MS;

    public static @NonNull ScanCriteria forAddress(@NonNull String address) {
        ScanCriteria criteria = new ScanCriteria();
        criteria.addAddress(address);
        criteria.setLimit(1);
        return criteria;
    }

    public ScanCriteria addAddress(@NonNull String address) {
        addresses.add(address);
        return this;
    }

    public ScanCriteria setScanRecord(@NonNull byte[] scanRecord) {
        this.scanRecord = scanRecord;
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

    public boolean doesScanRecordMatch(byte[] scanResponse) {
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
