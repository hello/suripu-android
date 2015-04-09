package is.hello.sense.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.BuildConfig;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.PersistentApiSessionManager;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;

@Module(
    library = true
)
@SuppressWarnings("UnusedDeclaration")
public class ApiModule {
    private final Context applicationContext;

    public static ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        if (!BuildConfig.DEBUG) {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        mapper.registerModule(new JodaModule());
        return mapper;
    }

    public ApiModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
    }

    @Provides @ApiAppContext Context providesApiApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides ApiSessionManager getApiSessionManager(@NonNull @ApiAppContext Context context,
                                                                @NonNull ObjectMapper mapper) {
        return new PersistentApiSessionManager(context, mapper);
    }

    @Singleton @Provides ObjectMapper provideObjectMapper() {
        return createConfiguredObjectMapper();
    }

    @Singleton @Provides Cache provideCache(@NonNull @ApiAppContext Context context) {
        File cacheDirectory = new File(context.getExternalCacheDir(), Constants.HTTP_CACHE_NAME);
        try {
            return new Cache(cacheDirectory, Constants.HTTP_CACHE_SIZE);
        } catch (IOException e) {
            Logger.warn(ApiModule.class.getSimpleName(), "Could not create local cache, ignoring.", e);
            return null;
        }
    }

    @Singleton @Provides OkHttpClient provideHttpClient(@NonNull Cache cache) {
        OkHttpClient client = new OkHttpClient();
        client.setCache(cache);
        client.setConnectTimeout(Constants.HTTP_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        client.setReadTimeout(Constants.HTTP_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        return client;
    }

    @Singleton @Provides RestAdapter provideRestAdapter(@NonNull ObjectMapper mapper,
                                                        @NonNull OkHttpClient httpClient,
                                                        @NonNull ApiSessionManager sessionManager) {
        RestAdapter.Builder builder = new RestAdapter.Builder();
        builder.setClient(new OkClient(httpClient));
        builder.setConverter(new ApiJacksonConverter(mapper));
        builder.setEndpoint(BuildConfig.BASE_URL);
        if (BuildConfig.DEBUG) {
            builder.setLogLevel(RestAdapter.LogLevel.FULL);
        } else {
            builder.setLogLevel(RestAdapter.LogLevel.BASIC);
        }
        builder.setLog(Logger.RETROFIT_LOGGER);
        builder.setErrorHandler(error -> {
            if (error.getKind() == RetrofitError.Kind.HTTP && error.getResponse().getStatus() == 401) {
                LocalBroadcastManager.getInstance(applicationContext)
                                     .sendBroadcast(new Intent(ApiSessionManager.ACTION_SESSION_INVALIDATED));
            }

            ErrorResponse errorResponse;
            try {
                errorResponse = (ErrorResponse) error.getBodyAs(ErrorResponse.class);
            } catch (Exception ignored) {
                errorResponse = null;
            }
            return new ApiException(errorResponse, error);
        });
        builder.setRequestInterceptor(request -> {
            if (sessionManager.hasSession()) {
                request.addHeader("Authorization", "Bearer " + sessionManager.getAccessToken());
            }
        });
        return builder.build();
    }

    @Singleton @Provides ApiService provideApiService(@NonNull RestAdapter adapter) {
        return adapter.create(ApiService.class);
    }
}
