package com.hello.ble.stack.transmission;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.util.BleUUID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by pangwu on 8/7/14.
 */
public class MorpheusBlePacketHandler extends BlePacketHandler {

    @Override
    public HelloBlePacket getHelloBlePacket(final UUID charUUID, byte[] blePacket) {
        if (BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID.equals(charUUID)) {
            int sequenceNumber = blePacket[0];
            return new HelloBlePacket(sequenceNumber, Arrays.copyOfRange(blePacket, 1, blePacket.length));
        } else {
            return new HelloBlePacket(-1, Arrays.copyOf(blePacket, blePacket.length));
        }
    }

    @Override
    public List<byte[]> prepareBlePacket(final byte[] applicationData) {
        int totalPacketCount = 1;
        if (applicationData.length > BlePacketHandler.HEADER_PACKET_PAYLOAD_LEN) {
            totalPacketCount = 1 + (applicationData.length - BlePacketHandler.HEADER_PACKET_PAYLOAD_LEN) / BlePacketHandler.PACKET_PAYLOAD_LEN;
            if (applicationData.length % BlePacketHandler.PACKET_PAYLOAD_LEN > 0) {
                totalPacketCount += 1;
            }
        }

        final ArrayList<byte[]> packets = new ArrayList<>();
        if (totalPacketCount == 1) {
            final byte[] headPacket = new byte[2 + applicationData.length];
            headPacket[1] = 1;
            for (int i = 0; i < applicationData.length; i++) {
                headPacket[i + 2] = applicationData[i];
            }
            packets.add(headPacket);
            return packets;
        }


        int bytesRemain = applicationData.length;
        for (int i = 0; i < totalPacketCount; i++) {
            if (i == 0) {
                final byte[] headPacket = new byte[BlePacketHandler.BLE_PACKET_LEN];
                headPacket[1] = (byte) totalPacketCount;
                for (int k = 0; k < BlePacketHandler.HEADER_PACKET_PAYLOAD_LEN; k++) {
                    headPacket[k + 2] = applicationData[k];
                    bytesRemain--;
                }
                packets.add(headPacket);

            }

            if (i > 0 && i < totalPacketCount - 1) {
                final byte[] packet = new byte[BlePacketHandler.BLE_PACKET_LEN];
                packet[0] = (byte) i;
                for (int k = 0; k < packet.length - 1; k++) {
                    packet[k + 1] = applicationData[BlePacketHandler.HEADER_PACKET_PAYLOAD_LEN + (i - 1) * BlePacketHandler.PACKET_PAYLOAD_LEN + k];
                    bytesRemain--;
                }
                packets.add(packet);
            }

            if (i == totalPacketCount - 1) {
                final byte[] packet = new byte[bytesRemain + 1];
                packet[0] = (byte) i;
                for (int k = 0; k < packet.length - 1; k++) {
                    packet[k + 1] = applicationData[BlePacketHandler.HEADER_PACKET_PAYLOAD_LEN + (i - 1) * BlePacketHandler.PACKET_PAYLOAD_LEN + k];
                    bytesRemain--;
                }
                packets.add(packet);
            }
        }

        return packets;
    }
}
