package is.hello.sense.bluetooth.devices;

import java.util.UUID;

public class SenseIdentifiers {
    public static final UUID SENSE_SERVICE = UUID.fromString("0000FEE1-1212-EFDE-1523-785FEABCD123");
    public static final byte[] SENSE_SERVICE_BYTES = {
            0x23, (byte) 0xD1, (byte) 0xBC, (byte) 0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte) 0xEF,   // EFDE
            0x12, 0x12,   // 1212
            (byte) 0xE1, (byte) 0xFE, 0x00, 0x00  // 0000FEE1
    };

    public static final UUID CHAR_PROTOBUF_COMMAND = UUID.fromString("0000BEEB-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_PROTOBUF_COMMAND_RESPONSE = UUID.fromString("0000B00B-0000-1000-8000-00805F9B34FB");

    public static final UUID DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

}
