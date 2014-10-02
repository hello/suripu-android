package com.hello.ble.stack.transmission;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.util.BleUUID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by pangwu on 7/31/14.
 */
public class PillBlePacketHandler extends BlePacketHandler {
    @Override
    public HelloBlePacket getHelloBlePacket(final UUID charUUID, byte[] blePacket) {
        if (BleUUID.CHAR_DATA_UUID.equals(charUUID) ||
                BleUUID.CHAR_COMMAND_RESPONSE_UUID.equals(charUUID) ||
                BleUUID.CHAR_DAY_DATETIME_UUID.equals(charUUID)) {
            int sequenceNumber = blePacket[0];
            return new HelloBlePacket(sequenceNumber, Arrays.copyOfRange(blePacket, 1, blePacket.length));
        } else {
            return new HelloBlePacket(-1, Arrays.copyOf(blePacket, blePacket.length));
        }
    }

    @Override
    public List<byte[]> prepareBlePacket(byte[] applicationData) {
        final ArrayList<byte[]> packets = new ArrayList<>();
        packets.add(applicationData);
        return packets;
    }
}
