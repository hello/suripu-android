package com.hello.ble.stack.application;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.BleUUID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by pangwu on 8/11/14.
 */
public class MotionStreamDataHandler extends HelloDataHandler<Long[]> {

    public MotionStreamDataHandler(final Pill sender) {
        super(sender);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        return BleUUID.CHAR_DATA_UUID.equals(charUUID);
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        final byte[] bytes = Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length);

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(byteArrayInputStream);
        final Long[] xyz = new Long[4];

        try {
            xyz[0] = (long) littleEndianDataInputStream.readShort();
            xyz[1] = (long) littleEndianDataInputStream.readShort();
            xyz[2] = (long) littleEndianDataInputStream.readShort();

            xyz[3] = (long) littleEndianDataInputStream.readUnsignedByte() << 0;
            xyz[3] += (long) littleEndianDataInputStream.readUnsignedByte() << 8;
            xyz[3] += (long) littleEndianDataInputStream.readUnsignedByte() << 16;
            xyz[3] += (long) littleEndianDataInputStream.readUnsignedByte() << 24;


            littleEndianDataInputStream.close();
            byteArrayInputStream.close();

            this.dataFinished(xyz);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


    }
}
