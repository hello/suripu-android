package is.hello.sense.bluetooth.devices.transmission;

import android.support.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
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
    private int packetsProcessed = 0;
    private int expectedIndex = 0;
    private byte[] buffer;
    private int bufferOffset = 0;
    private int bufferDataLength = 0;



    @Override
    public boolean shouldProcessCharacteristic(final @NonNull UUID characteristicIdentifier) {
        return SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE.equals(characteristicIdentifier);
    }

    @Override
    public void processPacket(final @NonNull SequencedPacket blePacket) {
        if (this.expectedIndex != blePacket.sequenceNumber) {
            this.packetsProcessed = 0;
            this.expectedIndex = 0;
            this.buffer = null;
            onError(new ProtobufProcessingError(ProtobufProcessingError.Reason.DATA_LOST_OR_OUT_OF_ORDER));
            return;
        } else {
            this.expectedIndex = blePacket.sequenceNumber + 1;
        }


        SequencedPacket lastPacket;
        if (blePacket.sequenceNumber == 0) {
            // Assume the packets arrive in order.
            this.packetsProcessed = 0;
            this.totalPackets = blePacket.payload[0];
            this.bufferOffset = 0;

            final SequencedPacket headPacket = new SequencedPacket(0, Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
            this.bufferDataLength = headPacket.payload.length;
            this.buffer = new byte[PacketHandler.HEADER_PACKET_PAYLOAD_LEN + PacketHandler.PACKET_PAYLOAD_LEN * (this.totalPackets - 1)];

            lastPacket = headPacket;
        } else {
            lastPacket = blePacket;
            this.bufferDataLength += blePacket.payload.length;
        }

        // copy data in packets to a continues payload buffer.
        if (this.bufferOffset < this.buffer.length) {
            System.arraycopy(
                /* src */ lastPacket.payload,
                /* srcStart */ 0,
                /* dest */ buffer,
                /* destStart */ bufferOffset,
                /* length */ lastPacket.payload.length
            );

            bufferOffset += lastPacket.payload.length;
        }
        packetsProcessed++;

        if (this.packetsProcessed == this.totalPackets) {
            final MorpheusBle.MorpheusCommand data;
            try {
                data = MorpheusBle.MorpheusCommand.parseFrom(Arrays.copyOfRange(this.buffer, 0, bufferDataLength));
                this.onResponse(data);
            } catch (InvalidProtocolBufferException e) {
                Logger.error(SensePacketDataHandler.class.getSimpleName(), "Could not parse command.", e);
                onError(new ProtobufProcessingError(ProtobufProcessingError.Reason.INVALID_PROTOBUF));
            }

            this.expectedIndex = 0;
            this.buffer = null;
            this.packetsProcessed = 0;
        }
    }
}
