package is.hello.sense.bluetooth.errors;

public class PeripheralServiceDiscoveryFailedError extends BluetoothError {
    public PeripheralServiceDiscoveryFailedError() {
        super("Could not perform service discovery on peripheral");
    }
}
