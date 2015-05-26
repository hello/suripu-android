package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.util.Logger;

/**
 * Responsible for encoding and decoding packets for the Bluetooth stack.
 *
 * @param <T> The type of value produced by the packet handler.
 */
public abstract class PacketHandler<T> {
    /**
     * The size of a standard Bluetooth packet.
     */
    public static final int BLE_PACKET_LENGTH = 20;

    private final PacketParser<T> packetParser;

    protected PacketHandler(@NonNull PacketParser<T> packetParser) {
        this.packetParser = packetParser;
    }

    /**
     * Divides a raw payload into packets suitable for transmission over BLE.
     */
    public abstract List<byte[]> createOutgoingPackets(final @NonNull byte[] payload);

    /**
     * Attempt to process an incoming packet from a characteristic.
     */
    public final void processIncomingPacket(final @NonNull UUID characteristicIdentifier, final @NonNull byte[] packet) {
        if (packetParser.canProcessPacket(characteristicIdentifier)) {
            packetParser.processPacket(characteristicIdentifier, packet);
        }
    }

    /**
     * Informs the packet handler and its parser that the Bluetooth transport has disconnected.
     */
    public final void transportDisconnected() {
        Logger.info(Peripheral.LOG_TAG, "onTransportDisconnected()");
        packetParser.onTransportDisconnected();
    }
}
