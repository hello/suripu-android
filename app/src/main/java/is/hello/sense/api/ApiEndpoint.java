package is.hello.sense.api;

import android.support.annotation.NonNull;

import is.hello.sense.BuildConfig;
import retrofit.Endpoint;

public class ApiEndpoint implements Endpoint {
    private final String clientId;
    private final String clientSecret;
    private final String url;

    public ApiEndpoint(@NonNull String clientId,
                       @NonNull String clientSecret,
                       @NonNull String url) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.url = url;
    }

    public ApiEndpoint() {
        this(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET, BuildConfig.BASE_URL);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return url;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
