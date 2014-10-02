package com.hello.ble;

/**
 * Created by pangwu on 7/2/14.
 */
public enum PillCommand {
    START_STREAM((byte) 0x01),
    STOP_STREAM((byte) 0x00),
    SET_TIME((byte) 0x06),
    GET_TIME((byte) 0x05),
    GET_DATA((byte) 0x04),
    CALIBRATE((byte) 0x02),
    GET_BATTERY_VOLT((byte) 0x07);

    private byte value;

    private PillCommand(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static PillCommand fromByte(final byte value) {
        switch (value) {
            case 0x02:
                return CALIBRATE;
            case 0x04:
                return GET_DATA;
            case 0x05:
                return GET_TIME;
            case 0x06:
                return SET_TIME;

            case 0x07:
                return GET_BATTERY_VOLT;
            default:
                return CALIBRATE;
        }
    }
}
