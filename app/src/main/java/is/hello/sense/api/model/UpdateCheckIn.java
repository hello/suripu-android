package is.hello.sense.api.model;

import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

import is.hello.sense.BuildConfig;

public class UpdateCheckIn extends ApiResponse {
    @JsonProperty("platform")
    public String getPlatform() {
        return "android";
    }

    @JsonProperty("app_version")
    public String getAppVersion() {
        return Integer.toString(BuildConfig.VERSION_CODE);
    }

    @JsonProperty("lang_code")
    public String getLanguageCode() {
        return Locale.getDefault().toString();
    }

    @Override
    public String toString() {
        return "CheckIn{" +
                "platform='" + getPlatform() + '\'' +
                ", appVersion='" + getAppVersion() + '\'' +
                ", languageCode='" + getLanguageCode() + '\'' +
                '}';
    }


    public static class Response extends ApiResponse {
        @JsonProperty("app_update_required")
        private boolean updateRequired;

        @JsonProperty("app_update_message")
        private String updateMessage;

        @JsonProperty("app_new_version")
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
