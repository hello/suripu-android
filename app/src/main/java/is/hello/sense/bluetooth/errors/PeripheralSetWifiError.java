package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;
import is.hello.sense.util.StringRef;

public class PeripheralSetWifiError extends BluetoothError implements Errors.Reporting {
    public final Reason reason;

    public PeripheralSetWifiError(@NonNull Reason reason) {
        super(reason.toString());

        this.reason = reason;
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return reason.toString();
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        switch (reason) {
            case MALFORMED_BYTES: {
                return StringRef.from(R.string.error_bluetooth_malformed_wifi_password);
            }

            case CONTAINS_NUL_BYTE: {
                return StringRef.from(R.string.error_bluetooth_wep_password_nul);
            }

            case EMPTY_PASSWORD: {
                return StringRef.from(R.string.error_bluetooth_empty_wifi_password);
            }

            default: {
                return StringRef.from(reason.toString());
            }
        }
    }

    public static enum Reason {
        MALFORMED_BYTES,
        CONTAINS_NUL_BYTE,
        EMPTY_PASSWORD,
    }
}
