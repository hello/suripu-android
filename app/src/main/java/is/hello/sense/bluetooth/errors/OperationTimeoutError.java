package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;
import is.hello.sense.util.StringRef;

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
    public String getContextInfo() {
        return operation.toString();
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.error_generic_bluetooth_timeout);
    }

    public static enum Operation {
        CONNECT,
        DISCOVER_SERVICES,
        SUBSCRIBE_NOTIFICATION,
        UNSUBSCRIBE_NOTIFICATION,
        WRITE_COMMAND,
        COMMAND_RESPONSE,
    }
}
