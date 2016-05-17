package is.hello.sense.api;

import android.support.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.BuildConfig;
import is.hello.sense.api.gson.ApiGsonConverter;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.util.Logger;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

@Module(complete = false, injects = {AccountSettingsFragment.class})
public class FacebookApiModule {

    @Provides AccessToken provideCurrentAccessToken(){
        return AccessToken.getCurrentAccessToken();
    }

    @Singleton
    @Provides
    @Named("facebook")
    RestAdapter provideFacebookRestAdapter(@NonNull Gson gson,
                                           @NonNull OkHttpClient httpClient,
                                           @NonNull ApiEndpoint endpoint,
                                           @NonNull AccessToken accessToken) {
        final RestAdapter.Builder builder = new RestAdapter.Builder();
        builder.setClient(new OkClient(httpClient));
        builder.setConverter(new ApiGsonConverter(gson));
        builder.setEndpoint(endpoint);
        if (BuildConfig.DEBUG) {
            builder.setLogLevel(RestAdapter.LogLevel.FULL);
        } else {
            builder.setLogLevel(RestAdapter.LogLevel.BASIC);
        }
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
            request.addHeader("Host", "graph.facebook.com");
            request.addHeader("Authorization", "Bearer " + accessToken.getToken());
        });
        return builder.build();
    }

    @Singleton @Provides FacebookApiService providesFacebookApiService(@Named("facebook") @NonNull RestAdapter adapter){
        return adapter.create(FacebookApiService.class);
    }

    @Provides
    @Singleton
    public CallbackManager providesCallbackManager(){
        return CallbackManager.Factory.create();
    }
}
