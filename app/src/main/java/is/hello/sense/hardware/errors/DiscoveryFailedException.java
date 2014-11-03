package is.hello.sense.hardware.errors;

public class DiscoveryFailedException extends BluetoothException {
    public DiscoveryFailedException() {
        super("Could not perform service discovery on device.");
    }
}
