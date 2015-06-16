package is.hello.sense.bluetooth.stacks.android;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
final class LegacyLePeripheralScanner implements Observable.OnSubscribe<List<Peripheral>>, BluetoothAdapter.LeScanCallback {
    private final @NonNull AndroidBluetoothStack stack;
    private final @NonNull PeripheralCriteria peripheralCriteria;
    private final @NonNull Map<String, ScannedPeripheral> results = new HashMap<>();
    private final boolean hasAddresses;

    private @Nullable Subscriber<? super List<Peripheral>> subscriber;
    private @Nullable Subscription timeout;
    private boolean scanning = false;

    LegacyLePeripheralScanner(@NonNull AndroidBluetoothStack stack, @NonNull PeripheralCriteria peripheralCriteria) {
        this.stack = stack;
        this.peripheralCriteria = peripheralCriteria;
        this.hasAddresses = !peripheralCriteria.peripheralAddresses.isEmpty();
    }


    @Override
    public void call(Subscriber<? super List<Peripheral>> subscriber) {
        Logger.info(BluetoothStack.LOG_TAG, "Beginning Scan (legacy impl)");

        this.subscriber = subscriber;

        this.scanning = stack.getAdapter().startLeScan(this);
        if (scanning) {
            this.timeout = stack.scheduler
                                .createWorker()
                                .schedule(this::onConcludeScan, peripheralCriteria.duration, TimeUnit.MILLISECONDS);
        } else {
            subscriber.onError(new BluetoothDisabledError());
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanResponse) {
        String address = device.getAddress();
        ScannedPeripheral existingResult = results.get(address);
        if (existingResult != null) {
            existingResult.rssi = rssi;
            return;
        }

        AdvertisingData advertisingData = AdvertisingData.parse(scanResponse);
        Logger.info(BluetoothStack.LOG_TAG, "Found device " + device.getName() + " - " + address + " " + advertisingData);

        if (!peripheralCriteria.matches(advertisingData)) {
            return;
        }

        if (hasAddresses && !peripheralCriteria.peripheralAddresses.contains(address)) {
            return;
        }

        results.put(address, new ScannedPeripheral(device, rssi, advertisingData));

        if (results.size() >= peripheralCriteria.limit) {
            Logger.info(BluetoothStack.LOG_TAG, "Discovery limit reached, concluding scan");
            onConcludeScan();
        }
    }

    public void onConcludeScan() {
        if (!scanning) {
            return;
        }

        this.scanning = false;

        // Low energy scanning on Android <=4.4.4 is broken when
        // a large number of unique peripherals have been scanned by
        // the device. This manifests on the client as a NPE within the
        // implementation of BluetoothAdapter#stopScan(LeScanCallback).
        // See <https://code.google.com/p/android/issues/detail?id=67272>
        try {
            stack.getAdapter().stopLeScan(this);
        } catch (Exception e) {
            Logger.warn(BluetoothStack.LOG_TAG, "Could not stop le scan due to internal stack error.", e);
        }

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        List<Peripheral> peripherals = new ArrayList<>();
        for (ScannedPeripheral scannedPeripheral : results.values()) {
            AndroidPeripheral peripheral = scannedPeripheral.createPeripheral(stack);
            peripherals.add(peripheral);
        }
        Logger.info(BluetoothStack.LOG_TAG, "Completed Scan " + peripherals);

        if (subscriber != null) {
            subscriber.onNext(peripherals);
            subscriber.onCompleted();
        } else {
            Logger.warn(BluetoothStack.LOG_TAG, "LegacyLePeripheralScanner invoked without a subscriber, ignoring.");
        }
    }
}
