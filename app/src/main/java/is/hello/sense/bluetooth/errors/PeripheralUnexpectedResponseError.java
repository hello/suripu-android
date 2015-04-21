package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.MorpheusCommand.CommandType;

public class PeripheralUnexpectedResponseError extends BluetoothError implements Errors.Reporting {
    public final CommandType expected;
    public final CommandType actual;

    public PeripheralUnexpectedResponseError(@NonNull CommandType expected,
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
    public Errors.Message getDisplayMessage() {
        return Errors.Message.from(R.string.error_message_unexpected_response);
    }
}
