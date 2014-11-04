package is.hello.sense.bluetooth.devices;

import java.util.UUID;

public class SenseIdentifiers {
    public static final UUID SENSE_SERVICE_UUID = UUID.fromString("0000FEE1-1212-EFDE-1523-785FEABCD123");

    public static final UUID CHAR_PROTOBUF_COMMAND_UUID = UUID.fromString("0000BEEB-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_PROTOBUF_COMMAND_RESPONSE_UUID = UUID.fromString("0000B00B-0000-1000-8000-00805F9B34FB");

    public static final UUID DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
}
