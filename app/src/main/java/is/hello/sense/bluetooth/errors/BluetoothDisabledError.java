package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;
import is.hello.sense.util.StringRef;

public class BluetoothDisabledError extends BluetoothError implements Errors.Reporting {
    public BluetoothDisabledError() {
        super("Bluetooth was disabled during an operation");
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return null;
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.error_bluetooth_disabled);
    }
}
