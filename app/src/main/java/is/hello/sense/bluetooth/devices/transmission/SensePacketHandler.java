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
        if (SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE.equals(characteristicIdentifier)) {
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


        int bytesRemaining = applicationData.length;
        for (int packetIndex = 0; packetIndex < totalPacketCount; packetIndex++) {
            if (packetIndex == 0) {
                final byte[] headPacket = new byte[PacketHandler.BLE_PACKET_LEN];
                headPacket[1] = (byte) totalPacketCount;

                System.arraycopy(applicationData, 0, headPacket, 2, PacketHandler.HEADER_PACKET_PAYLOAD_LEN);
                bytesRemaining -= PacketHandler.HEADER_PACKET_PAYLOAD_LEN;

                packets.add(headPacket);
            } else {
                final int packetLength = (packetIndex == totalPacketCount - 1) ? (bytesRemaining + 1) : PacketHandler.BLE_PACKET_LEN;
                final byte[] packet = new byte[packetLength];
                packet[0] = (byte) packetIndex;

                int dataStart = PacketHandler.HEADER_PACKET_PAYLOAD_LEN + (packetIndex - 1) * PacketHandler.PACKET_PAYLOAD_LEN;
                int dataAmount = packetLength - 1;
                System.arraycopy(applicationData, dataStart, packet, 1, dataAmount);
                bytesRemaining -= dataAmount;

                packets.add(packet);
            }
        }

        return packets;
    }
}
