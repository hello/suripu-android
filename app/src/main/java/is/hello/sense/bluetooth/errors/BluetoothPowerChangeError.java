package is.hello.sense.bluetooth.errors;

public class BluetoothPowerChangeError extends BluetoothError {
    public BluetoothPowerChangeError() {
        super("Could not alter Bluetooth power state");
    }
}
