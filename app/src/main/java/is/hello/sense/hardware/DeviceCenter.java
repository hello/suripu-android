package is.hello.sense.hardware;

import android.support.annotation.NonNull;

import java.util.List;

import rx.Observable;

public interface DeviceCenter {
    public static final long DEFAULT_SCAN_TIME_MS = (10 * 1000);

    @NonNull Observable<List<Device>> scanForDevices(@NonNull byte[] scanRecord, long timeoutMs);
    @NonNull Observable<Device> scanForDevice(@NonNull String address, long timeoutMs);
}
