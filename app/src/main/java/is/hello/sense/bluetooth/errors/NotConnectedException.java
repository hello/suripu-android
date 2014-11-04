package is.hello.sense.bluetooth.errors;

public class NotConnectedException extends BluetoothException {
    public NotConnectedException() {
    }

    public NotConnectedException(String detailMessage) {
        super(detailMessage);
    }

    public NotConnectedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NotConnectedException(Throwable throwable) {
        super(throwable);
    }
}
