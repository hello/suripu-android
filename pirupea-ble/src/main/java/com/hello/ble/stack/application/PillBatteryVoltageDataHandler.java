package com.hello.ble.stack.application;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.util.BleUUID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by pangwu on 8/15/14.
 */
public class PillBatteryVoltageDataHandler extends HelloDataHandler<Integer> {

    public PillBatteryVoltageDataHandler(final HelloBleDevice sender) {
        super(sender);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        return BleUUID.CHAR_COMMAND_RESPONSE_UUID.equals(charUUID);
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(blePacket.payload, 1, blePacket.payload.length - 1);
        final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(byteArrayInputStream);
        try {
            final Integer milliVolt = littleEndianDataInputStream.readInt();
            littleEndianDataInputStream.close();
            byteArrayInputStream.close();

            dataFinished(milliVolt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
