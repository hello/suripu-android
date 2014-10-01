package is.hello.sense.api;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.TransientApiSessionManager;
import is.hello.sense.util.Logger;
import retrofit.RequestInterceptor;
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

    @Singleton @Provides ApiSessionManager getApiSessionManager() {
        return new TransientApiSessionManager();
    }

    @Provides ObjectMapper provideObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }

    @Singleton @Provides RestAdapter provideRestAdapter(@NonNull ObjectMapper mapper, @NonNull final ApiSessionManager sessionManager) {
        RestAdapter.Builder builder = new RestAdapter.Builder();
        builder.setClient(new AndroidApacheClient());
        builder.setConverter(new JacksonConverter(mapper));
        builder.setEndpoint(ApiService.BASE_URL);
        builder.setLog(Logger.RETROFIT_LOGGER);
        builder.setRequestInterceptor(new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                if (sessionManager.hasSession())
                    request.addHeader("Authorization", "Bearer " + sessionManager.getAccessToken());
            }
        });
        return builder.build();
    }

    @Singleton @Provides ApiService provideApiService(@NonNull RestAdapter adapter) {
        return adapter.create(ApiService.class);
    }
}
