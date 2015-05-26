package is.hello.sense.bluetooth.devices.transmission;

import android.support.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.bluetooth.devices.transmission.protobuf.ProtobufProcessingError;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.stacks.transmission.PacketParser;
import is.hello.sense.util.Logger;

public class SensePacketParser extends PacketParser<SenseCommandProtos.MorpheusCommand> {
    //region Constants


    //endregion


    private int totalPackets = 0;
    private int packetsProcessed = 0;
    private int expectedIndex = 0;

    private byte[] buffer = null;
    private int bufferOffset = 0;
    private int bufferDataLength = 0;


    @Override
    public boolean canProcessPacket(final @NonNull UUID characteristicUUID) {
        return SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE.equals(characteristicUUID);
    }

    @Override
    public void processPacket(@NonNull UUID characteristicUUID, @NonNull byte[] packet) {
        int sequenceNumber = packet[0];
        if (this.expectedIndex != sequenceNumber) {
            cleanUp();

            dispatchError(new ProtobufProcessingError(ProtobufProcessingError.Reason.DATA_LOST_OR_OUT_OF_ORDER));

            return;
        } else {
            this.expectedIndex = sequenceNumber + 1;
        }


        int packetBufferStart;
        if (sequenceNumber == 0) {
            // Assume the packets arrive in order.
            this.packetsProcessed = 0;
            this.totalPackets = packet[1];
            this.bufferOffset = 0;

            packetBufferStart = SensePacketHandler.HEADER_PACKET_HEADER_LENGTH;

            int bufferSize = (SensePacketHandler.HEADER_PACKET_PAYLOAD_LENGTH +
                    SensePacketHandler.BODY_PACKET_PAYLOAD_LENGTH * (this.totalPackets - 1));
            this.buffer = new byte[bufferSize];
            this.bufferDataLength = (packet.length - packetBufferStart);
        } else {
            packetBufferStart = SensePacketHandler.BODY_PACKET_HEADER_LENGTH;
            this.bufferDataLength += (packet.length - packetBufferStart);
        }

        // copy data in packets to a continues payload buffer.
        if (this.bufferOffset < this.buffer.length) {
            int packetLength = (packet.length - packetBufferStart);
            System.arraycopy(
                /* src */ packet,
                /* srcStart */ packetBufferStart,
                /* dest */ buffer,
                /* destStart */ bufferOffset,
                /* length */ packetLength
            );

            bufferOffset += packetLength;
        }
        this.packetsProcessed++;

        if (this.packetsProcessed == this.totalPackets) {
            final SenseCommandProtos.MorpheusCommand data;
            try {
                // This particular Parser#parseFrom variant is not delegated in the generated MorpheusCommand.
                data = SenseCommandProtos.MorpheusCommand.PARSER.parseFrom(this.buffer, 0, bufferDataLength);
                this.dispatchResponse(data);
            } catch (InvalidProtocolBufferException e) {
                Logger.error(SensePacketParser.class.getSimpleName(), "Could not parse command.", e);
                dispatchError(new ProtobufProcessingError(ProtobufProcessingError.Reason.INVALID_PROTOBUF));
            }

            cleanUp();
        }
    }

    @Override
    protected void cleanUp() {
        this.totalPackets = 0;
        this.packetsProcessed = 0;
        this.expectedIndex = 0;

        this.buffer = null;
        this.bufferOffset = 0;
        this.bufferDataLength = 0;
    }
}
