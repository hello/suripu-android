package com.hello.ble.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.HelloBle;
import com.hello.ble.devices.HelloBleDevice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 7/22/14.
 */
public abstract class HelloBleDeviceScanner implements LeScanCallback {

    private final BluetoothAdapter bluetoothAdapter;
    private final String[] addresses;

    private final int maxScanTimeInMS;
    private final BleOperationCallback<Set<HelloBleDevice>> discoveryCallback;

    private final Set<HelloBleDevice> discoveredDevices = new HashSet<>();
    private final Map<String, BluetoothDevice> devices = new HashMap<String, BluetoothDevice>();

    private final Handler scanHandler;

    private final Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            bluetoothAdapter.stopLeScan(HelloBleDeviceScanner.this);

            for (final String address : devices.keySet()) {
                discoveredDevices.add(createDevice(HelloBle.getApplicationContext(), devices.get(address)));
            }
            discoveryCallback.onCompleted(null, discoveredDevices);
        }
    };

    public HelloBleDeviceScanner(final String[] addresses,
                                 final int maxScanTimeInMS,
                                 final BleOperationCallback<Set<HelloBleDevice>> discoveryCallback) {

        final Context context = HelloBle.getApplicationContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothAdapter = bluetoothAdapter;
        this.addresses = addresses;
        this.discoveryCallback = discoveryCallback;
        this.maxScanTimeInMS = maxScanTimeInMS;
        this.scanHandler = new Handler();

    }

    public abstract boolean isTargetDevice(final BluetoothDevice device, final byte[] scanResponse);

    public abstract HelloBleDevice createDevice(final Context context, final BluetoothDevice device);


    @Override
    public void onLeScan(final BluetoothDevice device, int i, byte[] scanRecord) {
        final String debugString = device.getName();

        if (!isTargetDevice(device, scanRecord)) {
            return;
        }

        if (this.devices.containsKey(device.getAddress())) {
            return;
        }


        if (this.addresses != null) {
            for (final String address : this.addresses) {
                if (!address.equals(device.getAddress())) {
                    continue;
                }

                this.devices.put(device.getAddress(), device);
            }

            if (this.devices.size() == this.addresses.length) {
                // We get the target devices, no need to wait until timeout.
                // Cancel the timeout callback and return.
                scanHandler.removeCallbacks(this.stopScanRunnable);
                this.stopScanRunnable.run();

            }
        } else {
            this.devices.put(device.getAddress(), device);
        }

    }

    public void beginDiscovery() {
        this.scanHandler.post(new Runnable() {
            @Override
            public void run() {
                HelloBleDeviceScanner.this.bluetoothAdapter.startLeScan(HelloBleDeviceScanner.this);
            }
        });

        scanHandler.postDelayed(this.stopScanRunnable, this.maxScanTimeInMS);
    }
}
