package is.hello.sense.bluetooth.devices.transmission;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.bluetooth.stacks.transmission.SequencedPacket;
import is.hello.sense.bluetooth.stacks.util.Bytes;

public class SensePacketHandlerTests extends TestCase {
    private final SensePacketHandler packetHandler = new SensePacketHandler();

    private static final byte[] LONG_SEQUENCE = {
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
    };

    public void testCreateSequencedPacket() throws Exception {
        byte[] packet = { 0x00, 0x01, 0x01 };
        SequencedPacket sequencedPacket = packetHandler.createSequencedPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, packet);
        assertEquals(sequencedPacket.sequenceNumber, 0);
        assertTrue(Arrays.equals(sequencedPacket.payload, new byte[] { 0x01, 0x01 }));

        SequencedPacket sequencedPacket2 = packetHandler.createSequencedPacket(UUID.randomUUID(), packet);
        assertEquals(sequencedPacket2.sequenceNumber, -1);
        assertTrue(Arrays.equals(sequencedPacket2.payload, packet));
    }

    public void testCreatePackets() throws Exception {
        List<byte[]> packets = packetHandler.createPackets(LONG_SEQUENCE);
        assertEquals(4, packets.size());
        int index = 0;
        for (byte[] packet : packets) {
            assertEquals(index++, packet[0]);
        }
    }

    public void testAllPacketsRightLength() throws Exception {
        byte[] failureCase = Bytes.fromString("08011002320832574952453137373A083257495245313737420A303132333435363738397803");
        List<byte[]> failureCasePackets = packetHandler.createPackets(failureCase);
        for (byte[] packet : failureCasePackets) {
            assertTrue(packet.length <= 20);
        }

        byte[] successCase = Bytes.fromString("080110023A083257495245313737420A303132333435363738397803");
        List<byte[]> successCasePackets = packetHandler.createPackets(successCase);
        for (byte[] packet : successCasePackets) {
            assertTrue(packet.length <= 20);
        }
    }
}
