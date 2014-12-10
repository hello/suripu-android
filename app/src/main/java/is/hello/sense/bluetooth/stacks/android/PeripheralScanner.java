package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

final class PeripheralScanner implements Observable.OnSubscribe<List<Peripheral>>, BluetoothAdapter.LeScanCallback {
    private final @NonNull AndroidBluetoothStack stack;
    private final @NonNull PeripheralCriteria peripheralCriteria;
    private final @NonNull Map<String, Pair<BluetoothDevice, Integer>> results = new HashMap<>();

    private @Nullable Subscriber<? super List<Peripheral>> subscriber;
    private @Nullable Subscription timeout;

    PeripheralScanner(@NonNull AndroidBluetoothStack stack, @NonNull PeripheralCriteria peripheralCriteria) {
        this.stack = stack;
        this.peripheralCriteria = peripheralCriteria;
    }


    @Override
    public void call(Subscriber<? super List<Peripheral>> subscriber) {
        Logger.info(BluetoothStack.LOG_TAG, "Beginning Scan");

        this.subscriber = subscriber;

        stack.getAdapter().startLeScan(this);
        this.timeout = stack.scheduler
                                   .createWorker()
                                   .schedule(this::onConcludeScan, peripheralCriteria.duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanResponse) {
        AdvertisingData advertisingData = AdvertisingData.parse(scanResponse);
        Logger.info(BluetoothStack.LOG_TAG, "Found device " + bluetoothDevice.getName() + " - " + bluetoothDevice.getAddress() + " " + advertisingData);

        if (!peripheralCriteria.matches(advertisingData)) {
            return;
        }

        if (!peripheralCriteria.peripheralAddresses.isEmpty() && !peripheralCriteria.peripheralAddresses.contains(bluetoothDevice.getAddress())) {
            return;
        }

        results.put(bluetoothDevice.getAddress(), Pair.create(bluetoothDevice, rssi));

        if (results.size() >= peripheralCriteria.limit) {
            Logger.info(BluetoothStack.LOG_TAG, "Discovery limit reached, concluding scan");
            onConcludeScan();
        }
    }

    public void onConcludeScan() {
        stack.getAdapter().stopLeScan(this);

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        List<Peripheral> peripherals = new ArrayList<>();
        for (Pair<BluetoothDevice, Integer> scanRecord : results.values()) {
            peripherals.add(new AndroidPeripheral(stack, scanRecord.first, scanRecord.second));
        }
        Logger.info(BluetoothStack.LOG_TAG, "Completed Scan " + peripherals);

        if (subscriber != null) {
            subscriber.onNext(peripherals);
            subscriber.onCompleted();
        } else {
            Logger.warn(BluetoothStack.LOG_TAG, "PeripheralScanner invoked without a subscriber, ignoring.");
        }
    }
}
