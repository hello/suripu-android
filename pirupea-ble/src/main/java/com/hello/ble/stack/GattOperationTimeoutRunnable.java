package com.hello.ble.stack;

import android.bluetooth.BluetoothGatt;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.BleOperationCallback.OperationFailReason;
import com.hello.ble.devices.HelloBleDevice;

/**
 * Created by pangwu on 7/15/14.
 */
class GattOperationTimeoutRunnable implements Runnable {

    private BluetoothGatt gatt;
    private BleOperationCallback callback;
    private HelloBleDevice sender;

    private GattOperationTimeoutRunnable() {
    }

    public GattOperationTimeoutRunnable(final HelloBleDevice sender,
                                        final BluetoothGatt gatt,
                                        final BleOperationCallback callback) {
        this.gatt = gatt;
        this.callback = callback;
        this.sender = sender;
    }

    @Override
    public void run() {
        if (this.gatt != null) {
            this.gatt.close();
        }

        // Although this is a timeout, we still need to make sure the
        // callback set by user is still called.
        if (this.callback != null && this.sender != null) {
            this.callback.onFailed(this.sender, OperationFailReason.TIME_OUT, 0);
        }
    }
}
