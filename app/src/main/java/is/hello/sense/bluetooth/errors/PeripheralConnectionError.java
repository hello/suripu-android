package is.hello.sense.bluetooth.errors;

public class PeripheralConnectionError extends BluetoothError {
    public PeripheralConnectionError() {
    }

    public PeripheralConnectionError(String detailMessage) {
        super(detailMessage);
    }

    public PeripheralConnectionError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeripheralConnectionError(Throwable throwable) {
        super(throwable);
    }
}
