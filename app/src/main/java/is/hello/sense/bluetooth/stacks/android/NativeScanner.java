package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.stacks.Device;
import is.hello.sense.bluetooth.stacks.DeviceCenter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;

public final class NativeScanner implements Observable.OnSubscribe<List<Device>>, BluetoothAdapter.LeScanCallback {
    private final @NonNull NativeDeviceCenter deviceCenter;
    private final @NonNull DeviceCenter.ScanCriteria scanCriteria;
    private final long timeoutMs;
    private final Map<String, Pair<BluetoothDevice, Integer>> results = new HashMap<>();

    private Subscriber<? super List<Device>> subscriber;

    public NativeScanner(@NonNull NativeDeviceCenter deviceCenter,
                         @NonNull DeviceCenter.ScanCriteria scanCriteria,
                         long timeoutMs) {
        this.deviceCenter = deviceCenter;
        this.scanCriteria = scanCriteria;
        this.timeoutMs = timeoutMs;
    }


    @Override
    public void call(Subscriber<? super List<Device>> subscriber) {
        Logger.info(DeviceCenter.LOG_TAG, "Beginning Scan");

        this.subscriber = subscriber;

        deviceCenter.adapter.startLeScan(this);
        deviceCenter.scheduler
                    .createWorker()
                    .schedule(this::onConcludeScan, timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanResponse) {
        Logger.info(DeviceCenter.LOG_TAG, "Found device " + bluetoothDevice.getName() + " - " + bluetoothDevice.getAddress());

        if (!scanCriteria.doesScanRecordMatch(scanResponse)) {
            return;
        }

        if (!scanCriteria.addresses.isEmpty() && !scanCriteria.addresses.contains(bluetoothDevice.getAddress())) {
            return;
        }

        if (results.size() >= scanCriteria.limit) {
            return;
        }

        results.put(bluetoothDevice.getAddress(), Pair.create(bluetoothDevice, rssi));
    }

    public void onConcludeScan() {
        deviceCenter.adapter.stopLeScan(this);

        List<Device> devices = new ArrayList<>();
        for (Pair<BluetoothDevice, Integer> scanRecord : results.values()) {
            devices.add(new NativeDevice(deviceCenter, scanRecord.first, scanRecord.second));
        }
        Logger.info(DeviceCenter.LOG_TAG, "Completed Scan " + devices);

        subscriber.onNext(devices);
        subscriber.onCompleted();
    }
}
