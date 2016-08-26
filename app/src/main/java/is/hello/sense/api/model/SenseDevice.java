package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

        @SerializedName("condition")
        public final String condition;


        public WiFiInfo(String ssid, int rssi, DateTime lastUpdated, String condition) {
            this.ssid = ssid;
            this.rssi = rssi;
            this.lastUpdated = lastUpdated;
            this.condition = condition;
        }

        public WiFiSignalStrength getSignalStrength() {
            if (rssi != 0){
                return WiFiSignalStrength.fromRssi(rssi);
            }
            return WiFiSignalStrength.fromCondition(condition);
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

    public static class SwapResponse extends ApiResponse {
        @SerializedName("status")
        public final SwapStatus status;

        public SwapResponse(final SwapStatus status){
            this.status = status;
        }

        public static Boolean isOK(@Nullable final SwapStatus swapStatus) {
            return SwapStatus.OK.equals(swapStatus);
        }
    }

    public enum SwapStatus implements Enums.FromString{
        OK,
        ACCOUNT_PAIRED_TO_MULTIPLE_SENSE,
        NEW_SENSE_PAIRED_TO_DIFFERENT_ACCOUNT,
        UNKNOWN;

        public static SwapStatus fromString(final String value){
            return Enums.fromString(value, values(), UNKNOWN);
        }
    }

    public static class SwapRequest {
        @SerializedName("sense_id")
        public final String senseId;

        public SwapRequest(@NonNull final String senseId){
            this.senseId = senseId;
        }
    }
}
