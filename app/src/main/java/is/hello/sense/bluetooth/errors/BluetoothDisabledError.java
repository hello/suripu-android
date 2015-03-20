package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;

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
    public Errors.Message getDisplayMessage() {
        return Errors.Message.from(R.string.error_bluetooth_disabled);
    }
}
