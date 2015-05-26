package is.hello.sense.bluetooth.devices.transmission;

import junit.framework.TestCase;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.util.LambdaVar;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.MorpheusCommand;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint;

public class SensePacketParserTests extends TestCase {
    private SensePacketParser packetParser;
    private SensePacketHandler packetHandler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.packetParser = new SensePacketParser();
        this.packetHandler = new SensePacketHandler(packetParser);
    }

    public void testShouldProcessCharacteristic() throws Exception {
        assertTrue(packetParser.shouldProcessCharacteristic(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE));
        assertFalse(packetParser.shouldProcessCharacteristic(UUID.fromString("D1700CFA-A6F8-47FC-92F5-9905D15F261C")));
    }

    public void testProcessPacketOutOfOrder() throws Exception {
        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        LambdaVar<Throwable> error = LambdaVar.empty();
        packetParser.onResponse = response::set;
        packetParser.onError = error::set;

        packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, new byte[] { 99 });

        assertNull(response.get());
        assertNotNull(error.get());
    }

    public void testStateCleanUp() throws Exception {
        byte[] testPacket = { 0, /* packetCount */ 2, 0x00 };

        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        packetParser.onResponse = response::set;
        LambdaVar<Throwable> error = LambdaVar.empty();
        packetParser.onError = error::set;

        packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, testPacket);

        assertNull(response.get());
        assertNull(error.get());


        packetHandler.transportDisconnected();


        response.clear();
        packetParser.onResponse = response::set;
        error.clear();
        packetParser.onError = error::set;

        packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, testPacket);

        assertNull(response.get());
        assertNull(error.get());
    }

    public void testProcessPacketInOrder() throws Exception {
        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_GET_WIFI_ENDPOINT)
                .setWifiSSID("Mostly Radiation")
                .setSecurityType(wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .setVersion(0)
                .build();

        List<byte[]> rawPackets = packetHandler.createRawPackets(morpheusCommand.toByteArray());

        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        packetParser.onResponse = response::set;

        LambdaVar<Throwable> error = LambdaVar.empty();
        packetParser.onError = error::set;

        for (byte[] packet : rawPackets) {
            packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, packet);
        }

        assertNull(error.get());
        assertNotNull(response.get());
        assertEquals("Mostly Radiation", response.get().getWifiSSID());
        assertEquals(wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN, response.get().getSecurityType());
    }
}
