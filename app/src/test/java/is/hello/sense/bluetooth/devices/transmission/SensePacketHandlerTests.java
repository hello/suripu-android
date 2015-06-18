package is.hello.sense.bluetooth.devices.transmission;

import org.junit.Test;

import java.util.List;

import is.hello.sense.bluetooth.stacks.util.Bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SensePacketHandlerTests {
    private final SensePacketParser packetParser = new SensePacketParser();
    private final SensePacketHandler packetHandler = new SensePacketHandler(packetParser);

    private static final byte[] LONG_SEQUENCE = {
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x07, 0x08, 0x09,
    };

    @Test
    public void createPackets() throws Exception {
        List<byte[]> packets = packetHandler.createOutgoingPackets(LONG_SEQUENCE);
        assertEquals(4, packets.size());
        int index = 0;
        for (byte[] packet : packets) {
            assertEquals(index++, packet[0]);
        }
    }

    @Test
    public void allPacketsRightLength() throws Exception {
        byte[] failureCase = Bytes.fromString("08011002320832574952453137373A083257495245313737420A303132333435363738397803");
        List<byte[]> failureCasePackets = packetHandler.createOutgoingPackets(failureCase);
        for (byte[] packet : failureCasePackets) {
            assertTrue(packet.length <= 20);
        }

        byte[] successCase = Bytes.fromString("080110023A083257495245313737420A303132333435363738397803");
        List<byte[]> successCasePackets = packetHandler.createOutgoingPackets(successCase);
        for (byte[] packet : successCasePackets) {
            assertTrue(packet.length <= 20);
        }
    }
}
