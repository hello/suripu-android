package is.hello.sense.bluetooth.sense.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.sense.bluetooth.sense.SenseIdentifiers;
import is.hello.sense.bluetooth.sense.errors.SenseProtobufError;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand;

/**
 * Implements packet division and parsing for
 * {@link is.hello.sense.bluetooth.sense.SensePeripheral}.
 */
public class SensePacketHandler implements GattPeripheral.PacketHandler {
    /**
     * The length of the first packet's header.
     *
     * <ol>
     *  <li><code>[0]: Packet sequence number</code></li>
     *  <li><code>[1]: Total packet count</code></li>
     * </ol>
     */
    public static final int HEADER_PACKET_HEADER_LENGTH = 2;

    /**
     * The length of the payload portion of the first packet.
     */
    public static final int HEADER_PACKET_PAYLOAD_LENGTH = 18;

    /**
     * The length of a body packet's header.
     *
     * <ol>
     *  <li><code>[0]: Packet sequence number</code></li>
     * </ol>
     */
    public static final int BODY_PACKET_HEADER_LENGTH = 1;

    /**
     * The length of the payload portion of a body packet.
     */
    public static final int BODY_PACKET_PAYLOAD_LENGTH = 19;

    /**
     * The parser of the packet handler.
     */
    @VisibleForTesting final Parser parser = new Parser();


    //region Primitive Methods

    /**
     * Divides a given payload into a series of packets that
     * can be transmitted over a BLE gatt connection.
     */
    public List<byte[]> createOutgoingPackets(@NonNull byte[] payload) {
        final ArrayList<byte[]> packets = new ArrayList<>();
        if (payload.length <= HEADER_PACKET_PAYLOAD_LENGTH) {
            final byte[] headPacket = new byte[2 + payload.length];
            headPacket[1] = 1;
            System.arraycopy(
                /* src */ payload,
                /* srcStart */ 0,
                /* dest */ headPacket,
                /* destStart */ 2,
                /* length */ payload.length
            );
            packets.add(headPacket);
        } else {
            int lengthNoHeader = (payload.length - HEADER_PACKET_PAYLOAD_LENGTH);
            int packetCount = (int) Math.ceil(1f + lengthNoHeader / (float) BODY_PACKET_PAYLOAD_LENGTH);

            int bytesRemaining = payload.length;
            for (int packetIndex = 0; packetIndex < packetCount; packetIndex++) {
                if (packetIndex == 0) {
                    final byte[] headerPacket = new byte[GattPeripheral.PacketHandler.PACKET_LENGTH];
                    headerPacket[0] = (byte) packetIndex;
                    headerPacket[1] = (byte) packetCount;

                    System.arraycopy(
                        /* src */ payload,
                        /* srcStart */ 0,
                        /* dest */ headerPacket,
                        /* destStart */ 2,
                        /* length */ HEADER_PACKET_PAYLOAD_LENGTH
                    );
                    bytesRemaining -= HEADER_PACKET_PAYLOAD_LENGTH;

                    packets.add(headerPacket);
                } else {
                    final int packetLength = (packetIndex == packetCount - 1) ? (bytesRemaining + 1) : GattPeripheral.PacketHandler.PACKET_LENGTH;
                    final byte[] packet = new byte[packetLength];
                    packet[0] = (byte) packetIndex;

                    int dataStart = HEADER_PACKET_PAYLOAD_LENGTH + (packetIndex - 1) * BODY_PACKET_PAYLOAD_LENGTH;
                    int dataAmount = packetLength - 1;
                    System.arraycopy(
                        /* src */ payload,
                        /* srcStart */ dataStart,
                        /* dest */ packet,
                        /* destStart */ 1,
                        /* length */ dataAmount
                    );
                    bytesRemaining -= dataAmount;

                    packets.add(packet);
                }
            }
        }

        return packets;
    }

    @Override
    public boolean processIncomingPacket(@NonNull UUID characteristicIdentifier, @NonNull byte[] payload) {
        if (parser.canProcessPacket(characteristicIdentifier)) {
            parser.processPacket(payload);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void transportDisconnected() {
        parser.onTransportDisconnected();
    }

    //endregion


    //region Response Listeners

    /**
     * Sets the response listener of the packet parser.
     */
    public void setResponseListener(@Nullable ResponseListener responseListener) {
        parser.responseListener = responseListener;
    }

    /**
     * Returns whether or not the parser has listener.
     * <p/>
     * The parser will only have listeners when there is a command in-flight.
     */
    public boolean hasResponseListener() {
        return (parser.responseListener != null);
    }

    /**
     * Represents an object interested in receiving values from the packet parser.
     */
    public interface ResponseListener {
        /**
         * Called when the packet parser has produced a complete value.
         */
        void onDataReady(MorpheusCommand response);

        /**
         * Called when the packet parser cannot produce a value
         * with the packets it has been receiving.
         */
        void onError(Throwable error);
    }

    //endregion


    /**
     * Responsible for decoding incoming packets. Separate from
     * the containing packet handler to simplify state isolation.
     */
    @VisibleForTesting
    static class Parser {
        private int totalPackets = 0;
        private int packetsProcessed = 0;
        private int expectedIndex = 0;

        private byte[] buffer = null;
        private int bufferOffset = 0;
        private int bufferDataLength = 0;


        //region Processing

        /**
         * Returns whether or not the parser can process a packet
         * coming from a specified characteristic.
         */
        boolean canProcessPacket(@NonNull UUID characteristicUUID) {
            return SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE.equals(characteristicUUID);
        }

        /**
         * Process a single packet from Sense.
         */
        void processPacket(@NonNull byte[] packet) {
            int sequenceNumber = packet[0];
            if (this.expectedIndex != sequenceNumber) {
                cleanUp();

                dispatchError(new SenseProtobufError(SenseProtobufError.Reason.DATA_LOST_OR_OUT_OF_ORDER));

                return;
            } else {
                this.expectedIndex = sequenceNumber + 1;
            }


            final int packetBufferStart;
            if (sequenceNumber == 0) {
                // Assume the packets arrive in order.
                this.packetsProcessed = 0;
                this.totalPackets = packet[1];
                this.bufferOffset = 0;

                packetBufferStart = HEADER_PACKET_HEADER_LENGTH;

                int bufferSize = (HEADER_PACKET_PAYLOAD_LENGTH +
                        BODY_PACKET_PAYLOAD_LENGTH * (this.totalPackets - 1));
                this.buffer = new byte[bufferSize];
                this.bufferDataLength = (packet.length - packetBufferStart);
            } else {
                packetBufferStart = BODY_PACKET_HEADER_LENGTH;
                this.bufferDataLength += (packet.length - packetBufferStart);
            }

            // copy data in packets to a continues payload buffer.
            if (this.bufferOffset < this.buffer.length) {
                int packetLength = (packet.length - packetBufferStart);
                System.arraycopy(
                    /* src */ packet,
                    /* srcStart */ packetBufferStart,
                    /* dest */ buffer,
                    /* destStart */ bufferOffset,
                    /* length */ packetLength
                );

                bufferOffset += packetLength;
            }
            this.packetsProcessed++;

            if (this.packetsProcessed == this.totalPackets) {
                final MorpheusCommand data;
                try {
                    // This particular Parser#parseFrom variant is not delegated in the generated MorpheusCommand.
                    data = MorpheusCommand.PARSER.parseFrom(this.buffer, 0, bufferDataLength);
                    this.dispatchData(data);
                } catch (InvalidProtocolBufferException e) {
                    dispatchError(new SenseProtobufError(SenseProtobufError.Reason.INVALID_PROTOBUF));
                }

                cleanUp();
            }
        }

        /**
         * Cleans up the parser's state either in response to an error,
         * disconnect, or successfully parsed response.
         */
        private void cleanUp() {
            this.totalPackets = 0;
            this.packetsProcessed = 0;
            this.expectedIndex = 0;

            this.buffer = null;
            this.bufferOffset = 0;
            this.bufferDataLength = 0;
        }

        //endregion


        //region Propagating Data

        @Nullable ResponseListener responseListener;

        /**
         * Pass off the given error to the registered response listener.
         */
        private void dispatchError(@NonNull Throwable error) {
            if (this.responseListener != null) {
                this.responseListener.onError(error);
                this.responseListener = null;
            }
        }

        /**
         * Pass off the fully parsed value to the registered response listener.
         */
        private void dispatchData(@Nullable MorpheusCommand response) {
            if (this.responseListener != null) {
                this.responseListener.onDataReady(response);
            }
        }

        /**
         * Informs the parser that the Bluetooth transport disconnected.
         */
        void onTransportDisconnected() {
            cleanUp();
            dispatchError(new BluetoothConnectionLostError());
        }

        //endregion
    }
}
