package is.hello.sense.api.model;

import android.os.Build;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public final class PushRegistration extends ApiResponse {
    public static final String OS_ANDROID = "android";

    @SerializedName("os")
    public final String os;

    @SerializedName("version")
    public final String version;

    @SerializedName("app_version")
    public final String appVersion;

    @SerializedName("token")
    public final String deviceToken;


    public PushRegistration(@NonNull String appVersion, @NonNull String deviceToken) {
        this.os = OS_ANDROID;
        this.version = Build.VERSION.RELEASE;
        this.appVersion = appVersion;
        this.deviceToken = deviceToken;
    }


    @Override
    public String toString() {
        return "PushRegistration{" +
                "os='" + os + '\'' +
                ", version='" + version + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                '}';
    }
}
