package is.hello.sense.bluetooth.devices.transmission;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.util.LambdaVar;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.MorpheusCommand;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SensePacketParserTests {
    private SensePacketParser packetParser;
    private SensePacketHandler packetHandler;

    @Before
    public void initialize() {
        this.packetParser = new SensePacketParser();
        this.packetHandler = new SensePacketHandler(packetParser);
    }

    @Test
    public void shouldProcessCharacteristic() throws Exception {
        assertTrue(packetParser.canProcessPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE));
        assertFalse(packetParser.canProcessPacket(UUID.fromString("D1700CFA-A6F8-47FC-92F5-9905D15F261C")));
    }

    @Test
    public void processPacketOutOfOrder() throws Exception {
        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        LambdaVar<Throwable> error = LambdaVar.empty();
        packetParser.responseListener = response::set;
        packetParser.errorListener = error::set;

        packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, new byte[] { 99 });

        assertNull(response.get());
        assertNotNull(error.get());
    }

    @Test
    public void stateCleanUp() throws Exception {
        byte[] testPacket = { 0, /* packetCount */ 2, 0x00 };

        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        packetParser.responseListener = response::set;
        LambdaVar<Throwable> error = LambdaVar.empty();
        packetParser.errorListener = error::set;

        packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, testPacket);

        assertNull(response.get());
        assertNull(error.get());


        packetHandler.transportDisconnected();


        response.clear();
        packetParser.responseListener = response::set;
        error.clear();
        packetParser.errorListener = error::set;

        packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, testPacket);

        assertNull(response.get());
        assertNull(error.get());
    }

    @Test
    public void processPacketInOrder() throws Exception {
        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_GET_WIFI_ENDPOINT)
                .setWifiSSID("Mostly Radiation")
                .setSecurityType(wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .setVersion(0)
                .build();

        List<byte[]> rawPackets = packetHandler.createOutgoingPackets(morpheusCommand.toByteArray());

        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        packetParser.responseListener = response::set;

        LambdaVar<Throwable> error = LambdaVar.empty();
        packetParser.errorListener = error::set;

        for (byte[] packet : rawPackets) {
            packetParser.processPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, packet);
        }

        assertNull(error.get());
        assertNotNull(response.get());
        assertEquals("Mostly Radiation", response.get().getWifiSSID());
        assertEquals(wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN, response.get().getSecurityType());
    }
}
