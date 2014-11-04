package is.hello.sense.bluetooth.errors;

public class DiscoveryFailedException extends BluetoothException {
    public DiscoveryFailedException() {
        super("Could not perform service discovery on device.");
    }
}
