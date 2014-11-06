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

import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.DiscoveryCriteria;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

final class PeripheralScanner implements Observable.OnSubscribe<List<Peripheral>>, BluetoothAdapter.LeScanCallback {
    private final @NonNull AndroidBluetoothStack deviceCenter;
    private final @NonNull
    DiscoveryCriteria discoveryCriteria;
    private final Map<String, Pair<BluetoothDevice, Integer>> results = new HashMap<>();

    private Subscriber<? super List<Peripheral>> subscriber;
    private Subscription timeout;

    PeripheralScanner(@NonNull AndroidBluetoothStack deviceCenter,
                      @NonNull DiscoveryCriteria discoveryCriteria) {
        this.deviceCenter = deviceCenter;
        this.discoveryCriteria = discoveryCriteria;
    }


    @Override
    public void call(Subscriber<? super List<Peripheral>> subscriber) {
        Logger.info(BluetoothStack.LOG_TAG, "Beginning Scan");

        this.subscriber = subscriber;

        deviceCenter.adapter.startLeScan(this);
        this.timeout = deviceCenter.scheduler
                                   .createWorker()
                                   .schedule(this::onConcludeScan, discoveryCriteria.duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanResponse) {
        Logger.info(BluetoothStack.LOG_TAG, "Found device " + bluetoothDevice.getName() + " - " + bluetoothDevice.getAddress());

        if (!discoveryCriteria.doesScanResponseMatch(scanResponse)) {
            return;
        }

        if (!discoveryCriteria.addresses.isEmpty() && !discoveryCriteria.addresses.contains(bluetoothDevice.getAddress())) {
            return;
        }

        results.put(bluetoothDevice.getAddress(), Pair.create(bluetoothDevice, rssi));

        if (results.size() >= discoveryCriteria.limit) {
            Logger.info(BluetoothStack.LOG_TAG, "Discovery limit reached, concluding scan");
            onConcludeScan();
        }
    }

    public void onConcludeScan() {
        deviceCenter.adapter.stopLeScan(this);

        timeout.unsubscribe();
        this.timeout = null;

        List<Peripheral> peripherals = new ArrayList<>();
        for (Pair<BluetoothDevice, Integer> scanRecord : results.values()) {
            peripherals.add(new AndroidPeripheral(deviceCenter, scanRecord.first, scanRecord.second));
        }
        Logger.info(BluetoothStack.LOG_TAG, "Completed Scan " + peripherals);

        subscriber.onNext(peripherals);
        subscriber.onCompleted();
    }
}
