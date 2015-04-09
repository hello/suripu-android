package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;

public class PeripheralConnectionError extends BluetoothError implements Errors.Reporting {
    public PeripheralConnectionError() {
        super("You are not connected");
    }

    public PeripheralConnectionError(String detailMessage) {
        super(detailMessage);
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return getMessage();
    }

    @NonNull
    @Override
    public Errors.Message getDisplayMessage() {
        return Errors.Message.from(R.string.error_bluetooth_no_connection);
    }
}
