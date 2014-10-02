package com.hello.ble.util;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Pill;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by pangwu on 8/7/14.
 */
public class PillScanner extends HelloBleDeviceScanner {

    private static final byte[] PILL_SERVICE_UUID_BYTES = new byte[]{
            0x23, (byte) 0xD1, (byte) 0xBC, (byte) 0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte) 0xEF,   // EFDE
            0x12, 0x12,   // 1212
            0x10, (byte) 0xE1, 0x00, 0x00  // 0000E110
    };

    public PillScanner(final String[] addresses,
                       final int maxScanTimeInMS,
                       final BleOperationCallback<Set<HelloBleDevice>> discoveryCallback) {
        super(addresses, maxScanTimeInMS, discoveryCallback);

    }

    @Override
    public boolean isTargetDevice(final BluetoothDevice device, final byte[] scanResponse) {
        if (scanResponse.length < PILL_SERVICE_UUID_BYTES.length) {
            return false;
        }

        boolean isPill = false;


        for (int i = 0; i < scanResponse.length - PILL_SERVICE_UUID_BYTES.length; i++) {

            final byte[] range = Arrays.copyOfRange(scanResponse, i, i + PILL_SERVICE_UUID_BYTES.length);
            if (Arrays.equals(range, PILL_SERVICE_UUID_BYTES)) {
                isPill = true;
                break;
            }

        }

        return isPill;
    }

    @Override
    public HelloBleDevice createDevice(final Context context, final BluetoothDevice device) {
        return new Pill(context, device);
    }
}
