package is.hello.sense.api;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.PersistentApiSessionManager;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Logger;
import retrofit.RestAdapter;
import retrofit.android.AndroidApacheClient;
import retrofit.converter.JacksonConverter;

@Module(
    library = true
)
@SuppressWarnings("UnusedDeclaration")
public class ApiModule {
    private final Context applicationContext;
    private final ApiEnvironment environment;
    private final BuildValues buildValues;

    public static ObjectMapper createConfiguredObjectMapper(@Nullable BuildValues buildValues) {
        ObjectMapper mapper = new ObjectMapper();
        if (buildValues == null || !buildValues.isDebugBuild()) {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        mapper.registerModule(new JodaModule());
        return mapper;
    }

    public ApiModule(@NonNull Context applicationContext,
                     @NonNull ApiEnvironment environment,
                     @NonNull BuildValues buildValues) {
        this.applicationContext = applicationContext.getApplicationContext();
        this.environment = environment;
        this.buildValues = buildValues;
    }

    @Provides @ApiAppContext Context providesApiApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides ApiSessionManager getApiSessionManager(@NonNull @ApiAppContext Context context,
                                                                @NonNull ObjectMapper mapper) {
        return new PersistentApiSessionManager(context, mapper);
    }

    @Singleton @Provides ObjectMapper provideObjectMapper() {
        return createConfiguredObjectMapper(buildValues);
    }

    @Singleton @Provides RestAdapter provideRestAdapter(@NonNull ObjectMapper mapper,
                                                        @NonNull ApiSessionManager sessionManager,
                                                        @NonNull BuildValues buildValues) {
        RestAdapter.Builder builder = new RestAdapter.Builder();
        builder.setClient(new AndroidApacheClient());
        builder.setConverter(new JacksonConverter(mapper));
        builder.setEndpoint(environment.baseUrl);
        if (buildValues.isDebugBuild())
            builder.setLogLevel(RestAdapter.LogLevel.FULL);
        else
            builder.setLogLevel(RestAdapter.LogLevel.BASIC);
        builder.setLog(Logger.RETROFIT_LOGGER);
        builder.setErrorHandler(error -> {
            ErrorResponse errorResponse;
            try {
                errorResponse = (ErrorResponse) error.getBodyAs(ErrorResponse.class);
            } catch (Exception ignored) {
                errorResponse = null;
            }
            return new ApiException(errorResponse, error);
        });
        builder.setRequestInterceptor(request -> {
            if (sessionManager.hasSession())
                request.addHeader("Authorization", "Bearer " + sessionManager.getAccessToken());
        });
        return builder.build();
    }

    @Singleton @Provides ApiService provideApiService(@NonNull RestAdapter adapter) {
        return adapter.create(ApiService.class);
    }

    @Provides ApiEnvironment provideEnvironment() {
        return environment;
    }

    @Provides BuildValues provideBuildValues() {
        return buildValues;
    }
}
