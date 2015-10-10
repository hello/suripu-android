package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public class SenseDevice extends BaseDevice {
    @SerializedName("color")
    public final Color color;

    @SerializedName("wifi_info")
    public final WiFiInfo wiFiInfo;

    public SenseDevice(State state,
                       Color color,
                       String deviceId,
                       String firmwareVersion,
                       DateTime lastUpdated,
                       WiFiInfo wiFiInfo) {
        super(state, deviceId, firmwareVersion, lastUpdated);

        this.color = color;
        this.wiFiInfo = wiFiInfo;
    }

    @Override
    public String toString() {
        return "Sense{" +
                "wiFiInfo=" + wiFiInfo +
                '}';
    }

    public static class WiFiInfo extends ApiResponse {
        @SerializedName("ssid")
        public final String ssid;

        @SerializedName("rssi")
        public final int rssi;

        @SerializedName("last_updated")
        public final DateTime lastUpdated;


        public WiFiInfo(String ssid, int rssi, DateTime lastUpdated) {
            this.ssid = ssid;
            this.rssi = rssi;
            this.lastUpdated = lastUpdated;
        }

        public WiFiSignalStrength getSignalStrength() {
            return WiFiSignalStrength.fromRssi(rssi);
        }


        @Override
        public String toString() {
            return "WiFiInfo{" +
                    "ssid='" + ssid + '\'' +
                    ", rssi=" + rssi +
                    ", lastUpdated=" + lastUpdated +
                    '}';
        }
    }

    public enum Color implements Enums.FromString {
        BLACK(R.string.device_color_black),
        WHITE(R.string.device_color_white),
        UNKNOWN(R.string.missing_data_placeholder);

        public final @StringRes
        int nameRes;

        Color(int nameRes) {
            this.nameRes = nameRes;
        }

        public static Color fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
