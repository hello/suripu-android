package is.hello.sense.bluetooth.errors;

public class OperationTimeoutError extends BluetoothError {
    public final Operation operation;

    public OperationTimeoutError(Operation operation, Throwable cause) {
        super("Operation " + operation + " timed out", cause);
        this.operation = operation;
    }

    public OperationTimeoutError(Operation operation) {
        this(operation, null);
    }

    public static enum Operation {
        DISCOVER_SERVICES,
        SUBSCRIBE_NOTIFICATION,
        UNSUBSCRIBE_NOTIFICATION,
        WRITE_COMMAND,
    }
}
