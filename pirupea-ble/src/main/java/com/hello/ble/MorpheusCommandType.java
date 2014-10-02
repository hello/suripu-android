package com.hello.ble;

/**
 * Created by pangwu on 8/6/14.
 */
public enum MorpheusCommandType {
    MORPHEUS_COMMAND_SET_TIME((byte) 0x00),
    MORPHEUS_COMMAND_GET_TIME((byte) 0x01),
    MORPHEUS_COMMAND_SET_WIFI_ENDPOINT((byte) 0x02),
    MORPHEUS_COMMAND_GET_WIFI_ENDPOINT((byte) 0x03),
    MORPHEUS_COMMAND_SET_ALARMS((byte) 0x04),
    MORPHEUS_COMMAND_GET_ALARMS((byte) 0x05),
    MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE((byte) 0x06),
    MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE((byte) 0x07),
    MORPHEUS_COMMAND_UNKNOWN((byte) 0xff);

    private byte value;

    private MorpheusCommandType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static MorpheusCommandType fromByte(final byte value) {
        switch (value) {
            case 0x00:
                return MORPHEUS_COMMAND_SET_TIME;
            case 0x01:
                return MORPHEUS_COMMAND_GET_TIME;
            case 0x02:
                return MORPHEUS_COMMAND_SET_WIFI_ENDPOINT;
            case 0x03:
                return MORPHEUS_COMMAND_GET_WIFI_ENDPOINT;
            case 0x04:
                return MORPHEUS_COMMAND_SET_ALARMS;
            case 0x05:
                return MORPHEUS_COMMAND_GET_ALARMS;
            case 0x06:
                return MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE;
            case 0x07:
                return MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE;
            default:
                return MORPHEUS_COMMAND_UNKNOWN;
        }
    }
}
