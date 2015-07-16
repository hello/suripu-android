package is.hello.sense.api.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

import is.hello.sense.BuildConfig;

public class UpdateCheckIn extends ApiResponse {
    @SerializedName("platform")
    public final String platform = "android";

    @SerializedName("app_version")
    public final String appVersion = Integer.toString(BuildConfig.VERSION_CODE);

    @SerializedName("lang_code")
    public final String languageCode = Locale.getDefault().toString();


    public static class Response extends ApiResponse {
        @SerializedName("app_update_required")
        private boolean updateRequired;

        @SerializedName("app_update_message")
        private String updateMessage;

        @SerializedName("app_new_version")
        private String newVersion;


        public boolean isUpdateRequired() {
            return updateRequired;
        }

        public String getUpdateMessage() {
            return updateMessage;
        }

        public String getNewVersion() {
            return newVersion;
        }

        public boolean isNewVersion() {
            return !TextUtils.equals(Integer.toString(BuildConfig.VERSION_CODE), getNewVersion());
        }


        @Override
        public String toString() {
            return "UpdateCheckIn.Response{" +
                    "updateRequired=" + updateRequired +
                    ", updateMessage='" + updateMessage + '\'' +
                    ", newVersion='" + newVersion + '\'' +
                    '}';
        }
    }
}
