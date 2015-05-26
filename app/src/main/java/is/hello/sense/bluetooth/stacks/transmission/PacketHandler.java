package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.util.Logger;

/**
 * This is the actual transmission layer wrap on BLE.
 */
public abstract class PacketHandler<T> {
    public static final int BLE_PACKET_LEN = 20;

    private final PacketParser<T> packetParser;

    protected PacketHandler(@NonNull PacketParser<T> packetParser) {
        this.packetParser = packetParser;
    }

    public abstract List<byte[]> createRawPackets(final @NonNull byte[] applicationData);

    public final void process(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] payload) {
        if (packetParser.shouldProcessCharacteristic(characteristicIdentifier)) {
            packetParser.processPacket(characteristicIdentifier, payload);
        }
    }

    public final void transportDisconnected() {
        Logger.info(Peripheral.LOG_TAG, "onTransportDisconnected()");
        packetParser.onTransportDisconnected();
    }
}
