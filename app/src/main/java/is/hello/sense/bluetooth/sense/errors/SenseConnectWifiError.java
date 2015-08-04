package is.hello.sense.bluetooth.sense.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BluetoothError;
import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.bluetooth.sense.model.SenseConnectToWiFiUpdate;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.wifi_connection_state;

public class SenseConnectWifiError extends BluetoothError implements Errors.Reporting {
    public final SenseConnectToWiFiUpdate status;

    /**
     * Returns whether or not a given connection status indicates an
     * error that cannot be recovered from by a connect retry on Sense.
     */
    public static boolean isImmediateError(@NonNull SenseConnectToWiFiUpdate status) {
        return (status.state == wifi_connection_state.SSL_FAIL ||
                status.state == wifi_connection_state.HELLO_KEY_FAIL);
    }

    public SenseConnectWifiError(@NonNull SenseConnectToWiFiUpdate status, @Nullable Throwable cause) {
        super(status.state.toString(), cause);
        this.status = status;
    }

    @Nullable
    @Override
    public String getContextInfo() {
        if (status.httpResponseCode != null) {
            return "Http Response Code: " + status.httpResponseCode;
        } else if (status.socketErrorCode != null) {
            return "Socket Error Code: " + status.socketErrorCode;
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        switch (status.state) {
            case SSL_FAIL:
                return StringRef.from(R.string.error_wifi_ssl_failure);

            case HELLO_KEY_FAIL:
                return StringRef.from(R.string.error_wifi_hello_key_failure);

            default:
                return StringRef.from(status.state.toString());
        }
    }
}
