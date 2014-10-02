package is.hello.sense.api;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.PersistentApiSessionManager;
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

    public ApiModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
    }

    @Provides Context providesApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides ApiSessionManager getApiSessionManager(@NonNull Context context, @NonNull ObjectMapper mapper) {
        return new PersistentApiSessionManager(context, mapper);
    }

    @Singleton @Provides ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

    @Singleton @Provides RestAdapter provideRestAdapter(@NonNull ObjectMapper mapper, @NonNull final ApiSessionManager sessionManager) {
        RestAdapter.Builder builder = new RestAdapter.Builder();
        builder.setClient(new AndroidApacheClient());
        builder.setConverter(new JacksonConverter(mapper));
        builder.setEndpoint(ApiService.BASE_URL);
        builder.setLogLevel(RestAdapter.LogLevel.FULL);
        //builder.setLog(Logger.RETROFIT_LOGGER);
        builder.setRequestInterceptor(request -> {
            if (sessionManager.hasSession())
                request.addHeader("Authorization", "Bearer " + sessionManager.getAccessToken());
        });
        return builder.build();
    }

    @Singleton @Provides ApiService provideApiService(@NonNull RestAdapter adapter) {
        return adapter.create(ApiService.class);
    }
}
