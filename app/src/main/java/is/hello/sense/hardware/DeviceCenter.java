package is.hello.sense.hardware;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import rx.Observable;

public interface DeviceCenter {
    public static final String LOG_TAG = "Bluetooth/" + DeviceCenter.class.getSimpleName();

    public static final long DEFAULT_SCAN_TIME_MS = (10 * 1000);

    @NonNull Observable<List<Device>> scanForDevice(@Nullable String address, @NonNull byte[] scanRecord, long timeoutMs);
}
