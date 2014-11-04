package is.hello.sense.bluetooth.stacks.transmission;

public class SequencedPacket {
    public final int sequenceNumber;
    public final byte[] payload;

    public SequencedPacket(final int sequenceNumber, final byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }
}
