package is.hello.sense.api.model;

import android.os.Build;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.BuildConfig;

public final class PushRegistration extends ApiResponse {
    public static final String OS_ANDROID = "android";

    @SerializedName("os")
    public final String os;

    /**
     * Specify Android OS version
     * ex 5.0 for Lollipop
     */
    @SerializedName("version")
    public final String version;

    @SerializedName("app_version")
    public final String appVersion;

    @SerializedName("token")
    public final String deviceToken;


    public PushRegistration(@NonNull final String deviceToken) {
        this.os = OS_ANDROID;
        this.version = Build.VERSION.RELEASE;
        this.appVersion = BuildConfig.VERSION_NAME;
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
