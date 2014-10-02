package com.hello.ble.util;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Morpheus;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by pangwu on 8/7/14.
 */
public class MorpheusScanner extends HelloBleDeviceScanner {
    private static final byte[] MORPHEUS_SERVICE_UUID_BYTES = new byte[]{
            0x23, (byte) 0xD1, (byte) 0xBC, (byte) 0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte) 0xEF,   // EFDE
            0x12, 0x12,   // 1212
            (byte) 0xE1, (byte) 0xFE, 0x00, 0x00  // 0000FEE1
    };

    public MorpheusScanner(final String[] addresses,
                           final int maxScanTimeInMS,
                           final BleOperationCallback<Set<HelloBleDevice>> discoveryCallback) {
        super(addresses, maxScanTimeInMS, discoveryCallback);

    }


    @Override
    public boolean isTargetDevice(final BluetoothDevice device, final byte[] scanResponse) {
        if (scanResponse.length < MORPHEUS_SERVICE_UUID_BYTES.length) {
            return false;
        }

        boolean isMorpheus = false;


        for (int i = 0; i < scanResponse.length - MORPHEUS_SERVICE_UUID_BYTES.length; i++) {

            final byte[] range = Arrays.copyOfRange(scanResponse, i, i + MORPHEUS_SERVICE_UUID_BYTES.length);
            if (Arrays.equals(range, MORPHEUS_SERVICE_UUID_BYTES)) {
                isMorpheus = true;
                break;
            }

        }

        return isMorpheus;
    }

    @Override
    public HelloBleDevice createDevice(final Context context, final BluetoothDevice device) {
        return new Morpheus(context, device);
    }
}
