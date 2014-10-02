package com.hello.ble.stack.application;

import android.util.Log;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.BleOperationCallback.OperationFailReason;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.PillMotionData;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.BleUUID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by pangwu on 7/14/14.
 */
public class MotionDataHandler extends HelloDataHandler<List<PillMotionData>> {

    private int totalPackets = 0;
    private byte[] buffer;
    private int bufferOffsetIndex = 0;
    private int unitLength = 16;

    private LinkedList<HelloBlePacket> packets = new LinkedList<>();
    private int expectedIndex = 0;

    public MotionDataHandler(final Pill sender) {
        super(sender);
    }

    public MotionDataHandler(final int unitLength, final Pill sender) {
        super(sender);

        this.unitLength = unitLength;
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        if (charUUID.equals(BleUUID.CHAR_DATA_UUID)) {
            return true;
        }

        return false;
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        if (this.expectedIndex != blePacket.sequenceNumber) {
            this.packets.clear();
            this.expectedIndex = 0;
            if (this.getDataCallback() != null) {
                this.getDataCallback().onFailed(this.getSender(), OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, -1);
            }
            return;
        } else {
            this.expectedIndex = blePacket.sequenceNumber + 1;
        }

        if (blePacket.sequenceNumber == 0) {
            // Assume the packets arrive in order.
            this.packets.clear();
            this.totalPackets = blePacket.payload[0] > 0 ? blePacket.payload[0] : 256 + blePacket.payload[0];
            this.bufferOffsetIndex = 0;

            final HelloBlePacket headPacket = new HelloBlePacket(0, Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
            this.packets.add(headPacket);

            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(headPacket.payload);
            final LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(byteArrayInputStream);

            try {
                final byte version = inputStream.readByte();
                final byte reserved1 = inputStream.readByte();
                final int structLength = inputStream.readUnsignedShort();

                this.buffer = new byte[structLength];
                inputStream.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        } else {
            this.packets.add(blePacket);
        }

        final HelloBlePacket lastPacket = this.packets.getLast();

        if (lastPacket != null) {
            for (int i = 0; (this.bufferOffsetIndex < this.buffer.length && i < lastPacket.payload.length); i++, this.bufferOffsetIndex++) {
                this.buffer[this.bufferOffsetIndex] = lastPacket.payload[i];
            }

            Log.i("Get data: ", this.packets.size() + " in " + this.totalPackets);

            if (this.packets.size() == this.totalPackets) {
                final List<PillMotionData> data = PillMotionData.fromBytes(this.buffer, this.unitLength);
                this.dataFinished(data);
            }
        }
    }

}
