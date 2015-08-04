package is.hello.sense.bluetooth.sense.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BluetoothError;
import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;

import static is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.MorpheusCommand.CommandType;

public class SenseUnexpectedResponseError extends BluetoothError implements Errors.Reporting {
    public final CommandType expected;
    public final CommandType actual;

    public SenseUnexpectedResponseError(@NonNull CommandType expected,
                                        @NonNull CommandType actual) {
        super("Expected '" + expected + "', got '" + actual + "' instead");

        this.expected = expected;
        this.actual = actual;
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return null;
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.error_message_unexpected_response);
    }
}
