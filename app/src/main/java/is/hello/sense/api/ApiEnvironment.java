package is.hello.sense.api;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.Enums;

public enum ApiEnvironment {
    STAGING("android_dev", "99999secret", "https://dev-api.hello.is/v1");

    public final String clientId;
    public final String clientSecret;
    public final String baseUrl;

    private ApiEnvironment(@NonNull String clientId, @NonNull String clientSecret, @NonNull String baseUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl;
    }

    public static ApiEnvironment fromString(@NonNull String name) {
        return Enums.fromString(name, values(), STAGING);
    }
}
