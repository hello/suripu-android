package is.hello.sense.bluetooth.errors;

public class BluetoothException extends RuntimeException {
    public BluetoothException() {
    }

    public BluetoothException(String detailMessage) {
        super(detailMessage);
    }

    public BluetoothException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BluetoothException(Throwable throwable) {
        super(throwable);
    }
}
