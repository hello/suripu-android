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
        final ArrayList<byte[]> packets = new ArrayList<>();
        if (applicationData.length <= PacketHandler.HEADER_PACKET_PAYLOAD_LEN) {
            final byte[] headPacket = new byte[2 + applicationData.length];
            headPacket[1] = 1;
            System.arraycopy(
                    /* src */ applicationData,
                    /* srcStart */ 0,
                    /* dest */ headPacket,
                    /* destStart */ 2,
                    /* length */ applicationData.length
            );
            packets.add(headPacket);
        } else {
            int lengthNoHeader = (applicationData.length - PacketHandler.HEADER_PACKET_PAYLOAD_LEN);
            int packetCount = (int) Math.ceil(1f + lengthNoHeader / (float) PacketHandler.PACKET_PAYLOAD_LEN);

            int bytesRemaining = applicationData.length;
            for (int packetIndex = 0; packetIndex < packetCount; packetIndex++) {
                if (packetIndex == 0) {
                    final byte[] headerPacket = new byte[PacketHandler.BLE_PACKET_LEN];
                    headerPacket[0] = (byte) packetIndex;
                    headerPacket[1] = (byte) packetCount;

                    System.arraycopy(
                        /* src */ applicationData,
                        /* srcStart */ 0,
                        /* dest */ headerPacket,
                        /* destStart */ 2,
                        /* length */ PacketHandler.HEADER_PACKET_PAYLOAD_LEN
                    );
                    bytesRemaining -= PacketHandler.HEADER_PACKET_PAYLOAD_LEN;

                    packets.add(headerPacket);
                } else {
                    final int packetLength = (packetIndex == packetCount - 1) ? (bytesRemaining + 1) : PacketHandler.BLE_PACKET_LEN;
                    final byte[] packet = new byte[packetLength];
                    packet[0] = (byte) packetIndex;

                    int dataStart = PacketHandler.HEADER_PACKET_PAYLOAD_LEN + (packetIndex - 1) * PacketHandler.PACKET_PAYLOAD_LEN;
                    int dataAmount = packetLength - 1;
                    System.arraycopy(
                        /* src */ applicationData,
                        /* srcStart */ dataStart,
                        /* dest */ packet,
                        /* destStart */ 1,
                        /* length */ dataAmount
                    );
                    bytesRemaining -= dataAmount;

                    packets.add(packet);
                }
            }
        }

        return packets;
    }
}
