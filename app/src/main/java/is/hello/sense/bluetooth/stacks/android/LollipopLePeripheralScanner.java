package is.hello.sense.bluetooth.stacks.android;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothLeScanError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LollipopLePeripheralScanner extends ScanCallback implements Observable.OnSubscribe<List<Peripheral>> {
    private final @NonNull AndroidBluetoothStack stack;
    private final @NonNull PeripheralCriteria peripheralCriteria;
    private final @NonNull BluetoothAdapter adapter;
    private final @Nullable BluetoothLeScanner scanner;
    private final @NonNull Map<String, ScannedPeripheral> results = new HashMap<>();
    private final boolean hasAddresses;

    private @Nullable Subscriber<? super List<Peripheral>> subscriber;
    private @Nullable Subscription timeout;
    private boolean scanning = false;

    LollipopLePeripheralScanner(@NonNull AndroidBluetoothStack stack, @NonNull PeripheralCriteria peripheralCriteria) {
        this.stack = stack;
        this.peripheralCriteria = peripheralCriteria;
        this.hasAddresses = !peripheralCriteria.peripheralAddresses.isEmpty();
        this.adapter = stack.getAdapter();
        this.scanner = adapter.getBluetoothLeScanner();
    }


    @Override
    public void call(Subscriber<? super List<Peripheral>> subscriber) {
        Logger.info(BluetoothStack.LOG_TAG, "Beginning Scan (Lollipop impl)");

        this.subscriber = subscriber;

        if (scanner != null) {
            this.scanning = true;
            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            scanner.startScan(null, builder.build(), this);

            this.timeout = stack.scheduler
                                .createWorker()
                                .schedule(this::onConcludeScan, peripheralCriteria.duration, TimeUnit.MILLISECONDS);
        } else {
            subscriber.onError(new BluetoothDisabledError());
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        Logger.info(BluetoothStack.LOG_TAG, "Forwarding batch results");

        for (ScanResult result : results) {
            onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (result.getScanRecord() == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        ScannedPeripheral existingResult = results.get(address);
        if (existingResult != null) {
            existingResult.rssi = result.getRssi();
            return;
        }

        byte[] scanResponse = result.getScanRecord().getBytes();
        AdvertisingData advertisingData = AdvertisingData.parse(scanResponse);
        Logger.info(BluetoothStack.LOG_TAG, "Found device " + device.getName() + " - " + address + " " + advertisingData);

        if (!peripheralCriteria.matches(advertisingData)) {
            return;
        }

        if (hasAddresses && !peripheralCriteria.peripheralAddresses.contains(address)) {
            return;
        }

        results.put(address, new ScannedPeripheral(device, result.getRssi(), advertisingData));

        if (results.size() >= peripheralCriteria.limit) {
            Logger.info(BluetoothStack.LOG_TAG, "Discovery limit reached, concluding scan");
            onConcludeScan();
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        this.scanning = false;

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        BluetoothLeScanError error = new BluetoothLeScanError(errorCode);
        if (subscriber != null) {
            subscriber.onError(error);
        } else {
            Logger.error(BluetoothStack.LOG_TAG, "LePeripheralScanner invoked without a subscriber.", error);
        }
    }

    public void onConcludeScan() {
        if (scanner == null) {
            throw new IllegalStateException("scanner is missing");
        }

        if (!scanning) {
            return;
        }

        this.scanning = false;

        boolean bluetoothOn = (adapter.getState() == BluetoothAdapter.STATE_ON);

        // The BluetoothLeScanner#stopScan(ScanCallback) method requires
        // that its associated BluetoothAdapter be in the on state to stop
        // the scan (how does this make sense?)
        if (bluetoothOn) {
            // State could conceivably change between getState and stopScan calls.
            try {
                scanner.stopScan(this);
            } catch (IllegalStateException e) {
                Logger.warn(BluetoothStack.LOG_TAG, "Adapter state changed between calls, ignoring.", e);
            }
        }

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        List<Peripheral> peripherals = new ArrayList<>();

        if (bluetoothOn) {
            for (ScannedPeripheral scannedPeripheral : results.values()) {
                AndroidPeripheral peripheral = scannedPeripheral.createPeripheral(stack);
                peripherals.add(peripheral);
            }
        }

        Logger.info(BluetoothStack.LOG_TAG, "Completed Scan " + peripherals);

        if (subscriber != null) {
            subscriber.onNext(peripherals);
            subscriber.onCompleted();
        } else {
            Logger.warn(BluetoothStack.LOG_TAG, "LePeripheralScanner invoked without a subscriber, ignoring.");
        }
    }
}
