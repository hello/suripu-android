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

    @SerializedName("hw_version")
    public final HardwareVersion hardwareVersion;

    public SenseDevice(final State state,
                       final Color color,
                       final String deviceId,
                       final String firmwareVersion,
                       final DateTime lastUpdated,
                       final WiFiInfo wiFiInfo,
                       final HardwareVersion hardwareVersion) {
        super(state, deviceId, firmwareVersion, lastUpdated);

        this.color = color;
        this.wiFiInfo = wiFiInfo;
        this.hardwareVersion = hardwareVersion;
    }

    @Override
    public int getDisplayTitleRes() {
        return HardwareVersion.UNKNOWN.equals(hardwareVersion) ?
                R.string.device_sense : hardwareVersion.nameRes;
    }

    @Override
    public String toString() {
        return "Sense{" +
                "wiFiInfo=" + wiFiInfo +
                '}';
    }

    public boolean shouldUpgrade() {
        return HardwareVersion.SENSE.equals(hardwareVersion);
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

        Color(final int nameRes) {
            this.nameRes = nameRes;
        }

        public static Color fromString(@NonNull final String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }

    public enum HardwareVersion implements Enums.FromString {
        SENSE(R.string.device_hardware_version_sense),
        SENSE_WITH_VOICE(R.string.device_hardware_version_sense_with_voice),
        UNKNOWN(R.string.device_hardware_version_unknown);

        public final @StringRes
        int nameRes;

        HardwareVersion(final int nameRes) {
            this.nameRes = nameRes;
        }

        public static HardwareVersion fromString(@NonNull final String string) {
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
