package com.hello.ble.stack.application;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.BleDateTimeConverter;
import com.hello.ble.util.BleUUID;

import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by pangwu on 7/14/14.
 */
public class TimeDataHandler extends HelloDataHandler<DateTime> {
    public TimeDataHandler(final Pill pill) {
        super(pill);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        if (charUUID.equals(BleUUID.CHAR_DAY_DATETIME_UUID)) {
            return true;
        }

        return false;
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        if (blePacket.sequenceNumber > 0) {
            throw new IllegalArgumentException("Invalid packet.");
        }

        final DateTime dateTime = BleDateTimeConverter.bleTimeToDateTime(
                Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
        this.dataFinished(dateTime);
    }

}
