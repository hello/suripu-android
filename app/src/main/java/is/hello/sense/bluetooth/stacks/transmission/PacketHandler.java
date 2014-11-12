package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This is the actual transmission layer wrap on BLE.
 */
public abstract class PacketHandler {
    public static int HEADER_PACKET_PAYLOAD_LEN = 18;
    public static int PACKET_PAYLOAD_LEN = 19;
    public static int BLE_PACKET_LEN = 20;

    private final Set<PacketDataHandler> dataHandlers = new HashSet<>();

    protected abstract SequencedPacket createSequencedPacket(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] payload);
    public abstract List<byte[]> createPackets(final @NonNull byte[] applicationData);

    public final void process(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] payload) {
        final SequencedPacket sequencedPacket = createSequencedPacket(characteristicIdentifier, payload);
        for (final PacketDataHandler handler : this.dataHandlers) {
            if (!handler.shouldProcessCharacteristic(characteristicIdentifier)) {
                continue;
            }

            handler.processPacket(sequencedPacket);
        }
    }

    public final void setPacketDataHandler(final PacketDataHandler handler) {
        this.dataHandlers.add(handler);
    }

    public final void unregisterDataHandler(final PacketDataHandler handler) {
        this.dataHandlers.remove(handler);
    }
}
