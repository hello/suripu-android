package is.hello.sense.bluetooth.errors;

/**
 * Super type of all errors yielded by the low level Bluetooth stack.
 *
 * @see PeripheralBondAlterationError
 * @see BluetoothGattError
 * @see is.hello.sense.bluetooth.errors.PeripheralConnectionError
 * @see is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError
 */
public class BluetoothError extends RuntimeException {
    public BluetoothError() {
    }

    public BluetoothError(String detailMessage) {
        super(detailMessage);
    }

    public BluetoothError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BluetoothError(Throwable throwable) {
        super(throwable);
    }
}
