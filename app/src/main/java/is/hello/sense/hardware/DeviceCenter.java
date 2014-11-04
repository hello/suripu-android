package is.hello.sense.hardware;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;

public interface DeviceCenter {
    public static final String LOG_TAG = "Bluetooth/" + DeviceCenter.class.getSimpleName();

    @NonNull Observable<List<Device>> scanForDevice(@NonNull ScanCriteria scanCriteria, long timeoutMs);


    public final class ScanCriteria {
        public List<String> addresses = new ArrayList<>();
        public byte[] scanRecord;
        public int limit = Integer.MAX_VALUE;

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
}
