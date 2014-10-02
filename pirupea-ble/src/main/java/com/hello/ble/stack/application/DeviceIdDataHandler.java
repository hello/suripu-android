package com.hello.ble.stack.application;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.util.BleUUID;

import java.util.UUID;

/**
 * Created by pangwu on 8/25/14.
 */
public class DeviceIdDataHandler extends HelloDataHandler<String> {
    public DeviceIdDataHandler(final HelloBleDevice sender) {
        super(sender);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        return BleUUID.CHAR_DEVICEID_UUID.equals(charUUID);
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        dataFinished(new String(blePacket.payload));
    }
}
