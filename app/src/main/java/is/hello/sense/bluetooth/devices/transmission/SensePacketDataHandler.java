package is.hello.sense.bluetooth.devices.transmission;

import android.support.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.devices.transmission.protobuf.ProtobufProcessingError;
import is.hello.sense.bluetooth.stacks.transmission.PacketDataHandler;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.transmission.SequencedPacket;
import is.hello.sense.util.Logger;

public class SensePacketDataHandler extends PacketDataHandler<MorpheusBle.MorpheusCommand> {
    private int totalPackets = 0;
    private byte[] buffer;
    private int expectedIndex = 0;
    private int actualDataLength = 0;
    private int bufferOffsetIndex = 0;

    private LinkedList<SequencedPacket> packets = new LinkedList<>();


    @Override
    public boolean shouldProcessCharacteristic(final @NonNull UUID characteristicIdentifier) {
        return SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE.equals(characteristicIdentifier);
    }

    @Override
    public void processPacket(final @NonNull SequencedPacket blePacket) {
        if (this.expectedIndex != blePacket.sequenceNumber) {
            this.packets.clear();
            this.expectedIndex = 0;
            this.buffer = null;
            onError(new ProtobufProcessingError(ProtobufProcessingError.Reason.DATA_LOST_OR_OUT_OF_ORDER));
            return;
        } else {
            this.expectedIndex = blePacket.sequenceNumber + 1;
        }


        if (blePacket.sequenceNumber == 0) {
            // Assume the packets arrive in order.
            this.packets.clear();
            this.totalPackets = blePacket.payload[0];
            this.bufferOffsetIndex = 0;

            final SequencedPacket headPacket = new SequencedPacket(0, Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
            this.packets.add(headPacket);
            this.actualDataLength = headPacket.payload.length;
            this.buffer = new byte[PacketHandler.HEADER_PACKET_PAYLOAD_LEN + PacketHandler.PACKET_PAYLOAD_LEN * (this.totalPackets - 1)];
        } else {
            this.packets.add(blePacket);
            this.actualDataLength += blePacket.payload.length;
        }

        final SequencedPacket lastPacket = this.packets.getLast();
        // copy data in packets to a continues payload buffer.
        for (int i = 0; (this.bufferOffsetIndex < this.buffer.length && i < lastPacket.payload.length); i++, this.bufferOffsetIndex++) {
            this.buffer[this.bufferOffsetIndex] = lastPacket.payload[i];
        }

        if (this.packets.size() == this.totalPackets) {
            final MorpheusBle.MorpheusCommand data;
            try {
                data = MorpheusBle.MorpheusCommand.parseFrom(Arrays.copyOfRange(this.buffer, 0, actualDataLength));
                this.onResponse(data);
            } catch (InvalidProtocolBufferException e) {
                Logger.error(SensePacketDataHandler.class.getSimpleName(), "Could not parse command.", e);
                onError(new ProtobufProcessingError(ProtobufProcessingError.Reason.INVALID_PROTOBUF));
            }

            this.packets.clear();
            this.expectedIndex = 0;
            this.buffer = null;
        }
    }
}
