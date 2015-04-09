package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import is.hello.sense.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.util.Logger;

/**
 * This is the actual transmission layer wrap on BLE.
 */
public abstract class PacketHandler {
    public static final int HEADER_PACKET_PAYLOAD_LEN = 18;
    public static final int PACKET_PAYLOAD_LEN = 19;
    public static final int BLE_PACKET_LEN = 20;

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

    public final void onTransportDisconnected() {
        Logger.info(Peripheral.LOG_TAG, "onTransportDisconnected()");

        BluetoothConnectionLostError error = new BluetoothConnectionLostError();
        for (PacketDataHandler<?> handler : dataHandlers) {
            handler.onError(error);
        }
    }

    public final void setPacketDataHandler(final PacketDataHandler handler) {
        this.dataHandlers.add(handler);
    }

    public final void unregisterDataHandler(final PacketDataHandler handler) {
        this.dataHandlers.remove(handler);
    }

}
