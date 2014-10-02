package com.hello.ble.stack.transmission;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.stack.application.HelloDataHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by pangwu on 7/31/14.
 * This is the actual transmission layer wrap on BLE.
 */
public abstract class BlePacketHandler {
    public static int HEADER_PACKET_PAYLOAD_LEN = 18;
    public static int PACKET_PAYLOAD_LEN = 19;
    public static int BLE_PACKET_LEN = 20;

    private final Set<HelloDataHandler> dataHandlers = new HashSet<>();

    protected abstract HelloBlePacket getHelloBlePacket(final UUID charUUID, final byte[] blePacket);

    public abstract List<byte[]> prepareBlePacket(final byte[] applicationData);

    public final void dispatch(final UUID charUUID, final byte[] blePacket) {
        final HelloBlePacket helloBlePacket = getHelloBlePacket(charUUID, blePacket);
        final ArrayList<HelloDataHandler> validHandlers = new ArrayList<HelloDataHandler>();


        for (final HelloDataHandler handler : this.dataHandlers) {
            if (!handler.shouldProcess(charUUID)) {
                continue;
            }

            validHandlers.add(handler);
        }

        for (final HelloDataHandler handler : validHandlers) {
            handler.onDataArrival(helloBlePacket);
        }
    }

    public final void registerDataHandler(final HelloDataHandler handler) {
        this.dataHandlers.add(handler);
    }

    public final void unregisterDataHandler(final HelloDataHandler handler) {
        this.dataHandlers.remove(handler);
    }
}
