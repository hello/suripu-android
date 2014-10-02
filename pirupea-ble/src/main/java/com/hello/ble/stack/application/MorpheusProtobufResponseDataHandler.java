package com.hello.ble.stack.application;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.ble.BleOperationCallback.OperationFailReason;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.protobuf.MorpheusBle.MorpheusCommand;
import com.hello.ble.stack.transmission.BlePacketHandler;
import com.hello.ble.util.BleUUID;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by pangwu on 8/7/14.
 */
public class MorpheusProtobufResponseDataHandler extends HelloDataHandler<MorpheusCommand> {

    private int totalPackets = 0;
    private byte[] buffer;
    private int expectedIndex = 0;
    private int actualDataLength = 0;
    private int bufferOffsetIndex = 0;

    private LinkedList<HelloBlePacket> packets = new LinkedList<>();


    public MorpheusProtobufResponseDataHandler(final HelloBleDevice helloBleDevice) {
        super(helloBleDevice);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        return BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID.equals(charUUID);
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
            this.totalPackets = blePacket.payload[0];
            this.bufferOffsetIndex = 0;

            final HelloBlePacket headPacket = new HelloBlePacket(0, Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
            this.packets.add(headPacket);
            this.actualDataLength = headPacket.payload.length;
            this.buffer = new byte[BlePacketHandler.HEADER_PACKET_PAYLOAD_LEN + BlePacketHandler.PACKET_PAYLOAD_LEN * (this.totalPackets - 1)];
        } else {
            this.packets.add(blePacket);
            this.actualDataLength += blePacket.payload.length;
        }

        final HelloBlePacket lastPacket = this.packets.getLast();
        // copy data in packets to a continues payload buffer.
        for (int i = 0; (this.bufferOffsetIndex < this.buffer.length && i < lastPacket.payload.length); i++, this.bufferOffsetIndex++) {
            this.buffer[this.bufferOffsetIndex] = lastPacket.payload[i];
        }

        if (this.packets.size() == this.totalPackets) {
            final MorpheusCommand data;
            try {
                data = MorpheusCommand.parseFrom(Arrays.copyOfRange(this.buffer, 0, actualDataLength));
                this.dataFinished(data);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                if (this.getDataCallback() != null) {
                    this.getDataCallback().onFailed(this.getSender(), OperationFailReason.INVALID_PROTOBUF, -1);
                }
            }

            this.packets.clear();
            this.expectedIndex = 0;
            this.buffer = null;
        }
    }
}
