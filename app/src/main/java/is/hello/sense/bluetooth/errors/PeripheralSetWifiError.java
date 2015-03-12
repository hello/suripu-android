package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;

public class PeripheralSetWifiError extends BluetoothError {
    public final Reason reason;

    public PeripheralSetWifiError(@NonNull Reason reason) {
        super(reason.toString());

        this.reason = reason;
    }

    public static enum Reason {
        MALFORMED_BYTES,
        CONTAINS_NUL_BYTE,
        EMPTY_PASSWORD,
    }
}
