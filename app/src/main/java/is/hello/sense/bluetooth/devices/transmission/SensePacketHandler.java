package is.hello.sense.bluetooth.devices.transmission;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.transmission.SequencedPacket;

public class SensePacketHandler extends PacketHandler {
    @Override
    public SequencedPacket createSequencedPacket(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] payload) {
        if (SenseIdentifiers.CHAR_PROTOBUF_COMMAND_RESPONSE.equals(characteristicIdentifier)) {
            int sequenceNumber = payload[0];
            return new SequencedPacket(sequenceNumber, Arrays.copyOfRange(payload, 1, payload.length));
        } else {
            return new SequencedPacket(-1, Arrays.copyOf(payload, payload.length));
        }
    }

    @Override
    public List<byte[]> createPackets(final @NonNull byte[] applicationData) {
        int totalPacketCount = 1;
        if (applicationData.length > PacketHandler.HEADER_PACKET_PAYLOAD_LEN) {
            totalPacketCount = 1 + (applicationData.length - PacketHandler.HEADER_PACKET_PAYLOAD_LEN) / PacketHandler.PACKET_PAYLOAD_LEN;
            if (applicationData.length % PacketHandler.PACKET_PAYLOAD_LEN > 0) {
                totalPacketCount += 1;
            }
        }

        final ArrayList<byte[]> packets = new ArrayList<>();
        if (totalPacketCount == 1) {
            final byte[] headPacket = new byte[2 + applicationData.length];
            headPacket[1] = 1;
            System.arraycopy(applicationData, 0, headPacket, 2, applicationData.length);
            packets.add(headPacket);
            return packets;
        }


        int bytesRemain = applicationData.length;
        for (int i = 0; i < totalPacketCount; i++) {
            if (i == 0) {
                final byte[] headPacket = new byte[PacketHandler.BLE_PACKET_LEN];
                headPacket[1] = (byte) totalPacketCount;
                for (int k = 0; k < PacketHandler.HEADER_PACKET_PAYLOAD_LEN; k++) {
                    headPacket[k + 2] = applicationData[k];
                    bytesRemain--;
                }
                packets.add(headPacket);

            }

            if (i > 0 && i < totalPacketCount - 1) {
                final byte[] packet = new byte[PacketHandler.BLE_PACKET_LEN];
                packet[0] = (byte) i;
                for (int k = 0; k < packet.length - 1; k++) {
                    packet[k + 1] = applicationData[PacketHandler.HEADER_PACKET_PAYLOAD_LEN + (i - 1) * PacketHandler.PACKET_PAYLOAD_LEN + k];
                    bytesRemain--;
                }
                packets.add(packet);
            }

            if (i == totalPacketCount - 1) {
                final byte[] packet = new byte[bytesRemain + 1];
                packet[0] = (byte) i;
                for (int k = 0; k < packet.length - 1; k++) {
                    packet[k + 1] = applicationData[PacketHandler.HEADER_PACKET_PAYLOAD_LEN + (i - 1) * PacketHandler.PACKET_PAYLOAD_LEN + k];
                    bytesRemain--;
                }
                packets.add(packet);
            }
        }

        return packets;
    }
}
