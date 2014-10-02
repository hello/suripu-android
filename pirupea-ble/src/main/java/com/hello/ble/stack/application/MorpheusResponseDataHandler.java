package com.hello.ble.stack.application;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.MorpheusCommandType;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.util.BleUUID;

import java.util.UUID;

/**
 * Created by pangwu on 8/7/14.
 */
public class MorpheusResponseDataHandler extends HelloDataHandler<MorpheusCommandType> {

    public MorpheusResponseDataHandler(final HelloBleDevice helloBleDevice) {
        super(helloBleDevice);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        return BleUUID.CHAR_COMMAND_RESPONSE_UUID.equals(charUUID);
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        final MorpheusCommandType command = MorpheusCommandType.fromByte(blePacket.payload[1]);
        this.dataFinished(command);
    }
}
