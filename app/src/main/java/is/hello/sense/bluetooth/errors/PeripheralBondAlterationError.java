package is.hello.sense.bluetooth.errors;

/**
 * Used to indicate that a bond could not be created or removed for a peripheral.
 * Generally indicates an unstable bluetooth service on the host device.
 */
public class PeripheralBondAlterationError extends BluetoothError {
    public PeripheralBondAlterationError() {
        super("Could not alter peripheral bond");
    }
}
