package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class PushRegistration extends ApiResponse {
    public static final String OS_ANDROID = "android";

    @JsonProperty("os")
    public final String os;

    @JsonProperty("version")
    public final String version;

    @JsonProperty("app_version")
    public final String appVersion;

    @JsonProperty("token")
    public final String deviceToken;


    public PushRegistration(String version, String appVersion, String deviceToken) {
        this.os = OS_ANDROID;
        this.version = version;
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
