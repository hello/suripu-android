package is.hello.sense.hardware.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import is.hello.sense.hardware.Device;
import rx.Observable;
import rx.Subscriber;

public final class NativeScanner implements Observable.OnSubscribe<List<Device>>, BluetoothAdapter.LeScanCallback {
    private final @NonNull NativeDeviceCenter deviceCenter;
    private final @Nullable String address;
    private final @NonNull byte[] scanRecord;
    private final long timeoutMs;
    private final Map<String, Device> results = new HashMap<>();

    private Subscriber<? super List<Device>> subscriber;

    public NativeScanner(@NonNull NativeDeviceCenter deviceCenter,
                         @Nullable String address,
                         @NonNull byte[] scanRecord,
                         long timeoutMs) {
        this.deviceCenter = deviceCenter;
        this.address = address;
        this.scanRecord = scanRecord;
        this.timeoutMs = timeoutMs;
    }


    @Override
    public void call(Subscriber<? super List<Device>> subscriber) {
        this.subscriber = subscriber;

        deviceCenter.adapter.startLeScan(this);
        deviceCenter.scheduler
                    .createWorker()
                    .schedule(this::onConcludeScan, timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        if (!Arrays.equals(scanRecord, this.scanRecord)) {
            return;
        }

        if (address != null && !address.equals(bluetoothDevice.getAddress())) {
            return;
        }

        results.put(bluetoothDevice.getAddress(), new NativeDevice(deviceCenter, bluetoothDevice, rssi));
    }

    public void onConcludeScan() {
        deviceCenter.adapter.stopLeScan(this);

        subscriber.onNext(new ArrayList<>(results.values()));
        subscriber.onCompleted();
    }
}
