package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.util.Logger;

/**
 * This is the actual transmission layer wrap on BLE.
 */
public abstract class PacketHandler {
    public static final int HEADER_PACKET_PAYLOAD_LEN = 18;
    public static final int PACKET_PAYLOAD_LEN = 19;
    public static final int BLE_PACKET_LEN = 20;

    private final PacketDataHandler<?> dataHandler;

    protected PacketHandler(@NonNull PacketDataHandler<?> dataHandler) {
        this.dataHandler = dataHandler;
    }

    protected abstract SequencedPacket createSequencedPacket(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] payload);
    public abstract List<byte[]> createPackets(final @NonNull byte[] applicationData);

    public final void process(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] payload) {
        final SequencedPacket sequencedPacket = createSequencedPacket(characteristicIdentifier, payload);
        if (dataHandler.shouldProcessCharacteristic(characteristicIdentifier)) {
            dataHandler.processPacket(sequencedPacket);
        }
    }

    public final void transportDisconnected() {
        Logger.info(Peripheral.LOG_TAG, "onTransportDisconnected()");
        dataHandler.onTransportDisconnected();
    }
}
