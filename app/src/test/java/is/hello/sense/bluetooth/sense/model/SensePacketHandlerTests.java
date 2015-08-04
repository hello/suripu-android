package is.hello.sense.bluetooth.sense.model;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.sense.bluetooth.sense.SenseIdentifiers;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand;
import is.hello.sense.graph.SenseTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SensePacketHandlerTests extends SenseTestCase {
    private final SensePacketHandler packetHandler = new SensePacketHandler();

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

    @Test
    public void shouldProcessCharacteristic() throws Exception {
        assertTrue(packetHandler.parser.canProcessPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE));
        assertFalse(packetHandler.parser.canProcessPacket(UUID.fromString("D1700CFA-A6F8-47FC-92F5-9905D15F261C")));
    }

    @Test
    public void processPacketOutOfOrder() throws Exception {
        TestResponseListener responseListener = new TestResponseListener();
        packetHandler.setResponseListener(responseListener);

        packetHandler.parser.processPacket(new byte[]{99});

        assertNull(responseListener.data);
        assertNotNull(responseListener.error);
    }

    @Test
    public void stateCleanUp() throws Exception {
        byte[] testPacket = { 0, /* packetCount */ 2, 0x00 };

        TestResponseListener responseListener = new TestResponseListener();
        packetHandler.setResponseListener(responseListener);

        packetHandler.parser.processPacket(testPacket);

        assertNull(responseListener.data);
        assertNull(responseListener.error);


        packetHandler.transportDisconnected();

        responseListener.reset();
        packetHandler.setResponseListener(responseListener);

        packetHandler.parser.processPacket(testPacket);

        assertNull(responseListener.data);
        assertNull(responseListener.error);
    }

    @Test
    public void processPacketInOrder() throws Exception {
        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_GET_WIFI_ENDPOINT)
                .setWifiSSID("Mostly Radiation")
                .setSecurityType(SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .setVersion(0)
                .build();

        List<byte[]> rawPackets = packetHandler.createOutgoingPackets(morpheusCommand.toByteArray());

        TestResponseListener responseListener = new TestResponseListener();
        packetHandler.setResponseListener(responseListener);

        for (byte[] packet : rawPackets) {
            packetHandler.parser.processPacket(packet);
        }

        assertNull(responseListener.error);
        assertNotNull(responseListener.data);
        assertEquals("Mostly Radiation", responseListener.data.getWifiSSID());
        assertEquals(SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN, responseListener.data.getSecurityType());
    }


    static class TestResponseListener implements SensePacketHandler.ResponseListener {
        MorpheusCommand data;
        Throwable error;

        void reset() {
            this.data = null;
            this.error = null;
        }

        @Override
        public void onDataReady(MorpheusCommand response) {
            this.data = response;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }
}
