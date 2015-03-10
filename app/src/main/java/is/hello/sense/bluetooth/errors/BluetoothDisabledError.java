package is.hello.sense.bluetooth.errors;

public class BluetoothDisabledError extends BluetoothError {
    public BluetoothDisabledError() {
        super("Bluetooth was disabled during an operation");
    }
}
