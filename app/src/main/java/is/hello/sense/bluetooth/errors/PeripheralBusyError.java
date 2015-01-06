package is.hello.sense.bluetooth.errors;

public class PeripheralBusyError extends BluetoothError {
    public PeripheralBusyError() {
        super("Bluetooth peripherals cannot run more than one command at once.");
    }
}
