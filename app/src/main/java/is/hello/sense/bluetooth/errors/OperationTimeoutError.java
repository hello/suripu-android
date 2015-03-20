package is.hello.sense.bluetooth.errors;

import android.support.annotation.Nullable;

import is.hello.sense.util.Errors;

public class OperationTimeoutError extends BluetoothError implements Errors.Reporting {
    public final Operation operation;

    public OperationTimeoutError(Operation operation, Throwable cause) {
        super("Operation " + operation + " timed out", cause);
        this.operation = operation;
    }

    public OperationTimeoutError(Operation operation) {
        this(operation, null);
    }

    @Nullable
    @Override
    public String getContext() {
        return operation.toString();
    }

    public static enum Operation {
        DISCOVER_SERVICES,
        SUBSCRIBE_NOTIFICATION,
        UNSUBSCRIBE_NOTIFICATION,
        WRITE_COMMAND,
        COMMAND_RESPONSE,
    }
}
