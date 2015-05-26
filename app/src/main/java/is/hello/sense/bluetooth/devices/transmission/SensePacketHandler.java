package is.hello.sense.bluetooth.devices.transmission;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.transmission.PacketParser;

public class SensePacketHandler extends PacketHandler<SenseCommandProtos.MorpheusCommand> {
    /**
     * The length of the first packet's header.
     *
     * <ol>
     *  <li><code>[0]: Packet sequence number</code></li>
     *  <li><code>[1]: Total packet count</code></li>
     * </ol>
     */
    public static final int HEADER_PACKET_HEADER_LENGTH = 2;

    /**
     * The length of the payload portion of the first packet.
     */
    public static final int HEADER_PACKET_PAYLOAD_LENGTH = 18;

    /**
     * The length of a body packet's header.
     *
     * <ol>
     *  <li><code>[0]: Packet sequence number</code></li>
     * </ol>
     */
    public static final int BODY_PACKET_HEADER_LENGTH = 1;

    /**
     * The length of the payload portion of a body packet.
     */
    public static final int BODY_PACKET_PAYLOAD_LENGTH = 19;


    public SensePacketHandler(@NonNull PacketParser<SenseCommandProtos.MorpheusCommand> dataHandler) {
        super(dataHandler);
    }

    @Override
    public List<byte[]> createRawPackets(final @NonNull byte[] applicationData) {
        final ArrayList<byte[]> packets = new ArrayList<>();
        if (applicationData.length <= HEADER_PACKET_PAYLOAD_LENGTH) {
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
            int lengthNoHeader = (applicationData.length - HEADER_PACKET_PAYLOAD_LENGTH);
            int packetCount = (int) Math.ceil(1f + lengthNoHeader / (float) BODY_PACKET_PAYLOAD_LENGTH);

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
                        /* length */ HEADER_PACKET_PAYLOAD_LENGTH
                    );
                    bytesRemaining -= HEADER_PACKET_PAYLOAD_LENGTH;

                    packets.add(headerPacket);
                } else {
                    final int packetLength = (packetIndex == packetCount - 1) ? (bytesRemaining + 1) : PacketHandler.BLE_PACKET_LEN;
                    final byte[] packet = new byte[packetLength];
                    packet[0] = (byte) packetIndex;

                    int dataStart = HEADER_PACKET_PAYLOAD_LENGTH + (packetIndex - 1) * BODY_PACKET_PAYLOAD_LENGTH;
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
