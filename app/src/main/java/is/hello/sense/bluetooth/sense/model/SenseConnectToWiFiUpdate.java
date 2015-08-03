package is.hello.sense.bluetooth.sense.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import is.hello.sense.bluetooth.sense.SensePeripheral;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.sense.model.protobuf.SenseCommandProtos.wifi_connection_state;

/**
 * Encapsulates status updates from
 * {@link SensePeripheral#connectToWiFiNetwork(String,
 * SenseCommandProtos.wifi_endpoint.sec_type, String)}.
 * Intended to be used to implement Analytic tracking for
 * WiFi connections on Sense.
 */
public final class SenseConnectToWiFiUpdate {
    /**
     * The current state of the WiFi connection operation.
     */
    public final @NonNull wifi_connection_state state;

    /**
     * The response code header from the final WiFi connection operation.
     * <p />
     * Known to be set when {@link #state} is {@link wifi_connection_state#CONNECTED}.
     */
    public final @Nullable String httpResponseCode;

    /**
     * The socket error code from the connection attempt.
     */
    public final @Nullable Integer socketErrorCode;


    public SenseConnectToWiFiUpdate(@NonNull wifi_connection_state state,
                                    @Nullable String httpResponseCode,
                                    @Nullable Integer socketErrorCode) {
        this.state = state;
        this.httpResponseCode = httpResponseCode;
        this.socketErrorCode = socketErrorCode;
    }

    public SenseConnectToWiFiUpdate(@NonNull SenseCommandProtos.MorpheusCommand response) {
        this.state = response.getWifiConnectionState();
        if (!TextUtils.isEmpty(response.getHttpResponseCode())) {
            this.httpResponseCode = response.getHttpResponseCode();
        } else {
            this.httpResponseCode = null;
        }
        if (response.hasSocketErrorCode()) {
            this.socketErrorCode = response.getSocketErrorCode();
        } else {
            this.socketErrorCode = null;
        }
    }


    @Override
    public String toString() {
        return "SenseConnectWiFiStatus{" +
                "state=" + state +
                ", httpResponseCode='" + httpResponseCode + '\'' +
                ", socketErrorCode=" + socketErrorCode +
                '}';
    }
}
