package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;

public class BluetoothPowerChangeError extends BluetoothError implements Errors.Reporting {
    public BluetoothPowerChangeError() {
        super("Could not alter Bluetooth power state");
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return null;
    }

    @NonNull
    @Override
    public Errors.Message getDisplayMessage() {
        return Errors.Message.from(R.string.error_bluetooth_power_change);
    }
}
