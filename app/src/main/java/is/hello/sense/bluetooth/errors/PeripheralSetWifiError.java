package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.util.Errors;

public class PeripheralSetWifiError extends BluetoothError implements Errors.Reporting {
    public final Reason reason;

    public PeripheralSetWifiError(@NonNull Reason reason) {
        super(reason.toString());

        this.reason = reason;
    }

    @Nullable
    @Override
    public String getContext() {
        return reason.toString();
    }

    public static enum Reason {
        MALFORMED_BYTES,
        CONTAINS_NUL_BYTE,
        EMPTY_PASSWORD,
    }
}
