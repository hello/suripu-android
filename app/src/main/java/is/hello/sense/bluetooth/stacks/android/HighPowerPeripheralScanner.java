package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;

class HighPowerPeripheralScanner extends BroadcastReceiver implements Observable.OnSubscribe<List<BluetoothDevice>> {
    /**
     * Roughly how long the documentation says a scan should take.
     */
    private static final int SCAN_DURATION_S = 15;

    private final Context context;
    private final BluetoothAdapter adapter;
    private final Scheduler.Worker worker;

    private @Nullable final List<BluetoothDevice> devices;
    private @Nullable Subscriber<? super List<BluetoothDevice>> subscriber;
    private @Nullable Subscription timeout;

    private boolean discovering = false;
    private boolean registered = false;

    //region Lifecycle

    HighPowerPeripheralScanner(@NonNull AndroidBluetoothStack stack, boolean saveResults) {
        this.context = stack.applicationContext;
        this.adapter = stack.getAdapter();
        this.worker = stack.scheduler.createWorker();

        if (saveResults) {
            this.devices = new ArrayList<>();
        } else {
            this.devices = null;
        }
    }

    @Override
    public void call(Subscriber<? super List<BluetoothDevice>> subscriber) {
        this.subscriber = subscriber;

        startDiscovery();

        // This is only necessary on some (Samsung?) devices.
        this.timeout = worker.schedule(() -> {
            if (timeout != null && !timeout.isUnsubscribed()) {
                stopDiscovery();
            }
        }, SCAN_DURATION_S, TimeUnit.SECONDS);
    }

    //endregion


    //region Callbacks

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BluetoothDevice.ACTION_FOUND: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                ParcelUuid uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                worker.schedule(() -> onDeviceFound(device, rssi, uuid, name));

                break;
            }

            case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                worker.schedule(this::onDiscoveryStarted);
                break;
            }

            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                worker.schedule(this::onDiscoveryFinished);
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown intent " + intent);
            }
        }
    }

    private void onDeviceFound(@NonNull BluetoothDevice device,
                               short rssi,
                               @Nullable ParcelUuid uuid,
                               @Nullable String name) {
        Logger.info(BluetoothStack.LOG_TAG, "high power scan found {" + device + " rssi: " + rssi + ", uuid: " + uuid + ", name: " + name + "}");
        if (devices != null) {
            devices.add(device);
        }
    }

    private void onDiscoveryStarted() {
        Logger.info(BluetoothStack.LOG_TAG, "high power scan started");
        this.discovering = true;
        if (devices != null) {
            devices.clear();
        }
    }

    private void onDiscoveryFinished() {
        Logger.info(BluetoothStack.LOG_TAG, "high power scan finished");

        this.discovering = false;
        stopDiscovery();
    }

    //endregion

    public void startDiscovery() {
        Logger.info(BluetoothStack.LOG_TAG, "start high power scan");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(this, intentFilter);
        this.registered = true;

        if (!adapter.isDiscovering() && !adapter.startDiscovery()) {
            Logger.error(BluetoothStack.LOG_TAG, "Could not start discovery");

            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onError(new PeripheralNotFoundError());
                this.subscriber = null;
            }
        }
    }

    public void stopDiscovery() {
        if (!registered) {
            return;
        }

        Logger.info(BluetoothStack.LOG_TAG, "stop high power scan");

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        context.unregisterReceiver(this);
        this.registered = false;

        if (discovering && !adapter.cancelDiscovery()) {
            Logger.error(BluetoothStack.LOG_TAG, "Could not stop discovery");

            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onError(new PeripheralNotFoundError());
                this.subscriber = null;
            }
        }

        if (subscriber != null && !subscriber.isUnsubscribed()) {
            if (devices != null) {
                subscriber.onNext(devices);
            } else {
                subscriber.onNext(Collections.<BluetoothDevice>emptyList());
            }
            subscriber.onCompleted();

            this.subscriber = null;
        }
    }
}
