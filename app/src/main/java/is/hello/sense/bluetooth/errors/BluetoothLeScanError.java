package is.hello.sense.bluetooth.errors;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanCallback;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.util.Errors;
import is.hello.sense.util.StringRef;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeScanError extends BluetoothError implements Errors.Reporting {
    private final int errorCode;

    public static String stringFromErrorCode(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "SCAN_FAILED_ALREADY_STARTED";

            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";

            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "SCAN_FAILED_INTERNAL_ERROR";

            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "SCAN_FAILED_FEATURE_UNSUPPORTED";

            default:
                return "UNKNOWN: " + errorCode;
        }
    }

    public BluetoothLeScanError(int errorCode) {
        super(stringFromErrorCode(errorCode));

        this.errorCode = errorCode;
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return stringFromErrorCode(errorCode);
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.error_peripheral_scan_failure);
    }
}
