package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

public class SenseDevice extends BaseDevice {
    @SerializedName("wifi_info")
    public final WiFiInfo wiFiInfo;

    public SenseDevice(@NonNull State state,
                       @NonNull Color color,
                       @NonNull String id,
                       @NonNull String firmwareVersion,
                       @NonNull DateTime lastUpdated,
                       @NonNull WiFiInfo wiFiInfo) {
        super(state, color, id, firmwareVersion, lastUpdated);

        this.wiFiInfo = wiFiInfo;
    }

    @Override
    public String toString() {
        return "Sense{" +
                "wiFiInfo=" + wiFiInfo +
                '}';
    }

    public static class WiFiInfo {
        @SerializedName("ssid")
        public final String ssid;

        @SerializedName("rssi")
        public final long rssi;

        @SerializedName("last_updated")
        public final DateTime lastUpdated;

        @SerializedName("condition")
        public final String condition;


        public WiFiInfo(@NonNull String ssid,
                        long rssi,
                        @NonNull DateTime lastUpdated,
                        @NonNull String condition) {
            this.ssid = ssid;
            this.rssi = rssi;
            this.lastUpdated = lastUpdated;
            this.condition = condition;
        }


        @Override
        public String toString() {
            return "WiFiInfo{" +
                    "ssid='" + ssid + '\'' +
                    ", rssi=" + rssi +
                    ", lastUpdated=" + lastUpdated +
                    ", condition=" + condition +
                    '}';
        }
    }
}
