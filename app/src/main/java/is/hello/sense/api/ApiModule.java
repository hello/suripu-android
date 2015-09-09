package is.hello.sense.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.gson.ApiGsonConverter;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.gson.ExcludeExclusionStrategy;
import is.hello.sense.api.gson.GsonJodaTime;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.PersistentApiSessionManager;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.markup.MarkupDeserializer;
import is.hello.sense.util.markup.MarkupProcessor;
import is.hello.sense.util.markup.text.MarkupString;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;

@Module(
    library = true
)
@SuppressWarnings("UnusedDeclaration")
public class ApiModule {
    private final Context applicationContext;

    public static Gson createConfiguredGson(@NonNull MarkupProcessor markupProcessor) {
        GsonBuilder builder = new GsonBuilder();
        builder.addSerializationExclusionStrategy(new ExcludeExclusionStrategy());

        builder.registerTypeAdapter(new TypeToken<DateTime>(){}.getType(),
                new GsonJodaTime.DateTimeSerialization(ISODateTimeFormat.dateTime().withZoneUTC(),
                        GsonJodaTime.SerializeAs.NUMBER));

        builder.registerTypeAdapter(new TypeToken<LocalDate>(){}.getType(),
                new GsonJodaTime.LocalDateSerialization(DateTimeFormat.forPattern(ApiService.DATE_FORMAT)));

        builder.registerTypeAdapter(new TypeToken<LocalTime>(){}.getType(),
                new GsonJodaTime.LocalTimeSerialization(DateTimeFormat.forPattern(ApiService.TIME_FORMAT)));

        builder.registerTypeAdapter(new TypeToken<MarkupString>(){}.getType(),
                new MarkupDeserializer(markupProcessor));

        builder.registerTypeHierarchyAdapter(Enums.FromString.class, new Enums.Serialization());

        return builder.create();
    }

    public ApiModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
    }

    @Provides @ApiAppContext Context provideApiApplicationContext() {
        return applicationContext;
    }

    @Provides ApiEndpoint provideApiEndpoint() {
        if (BuildConfig.DEBUG) {
            return new DynamicApiEndpoint(applicationContext);
        } else {
            return new ApiEndpoint();
        }
    }

    @Singleton @Provides ApiSessionManager provideApiSessionManager(@NonNull @ApiAppContext Context context,
                                                                    @NonNull Gson gson) {
        return new PersistentApiSessionManager(context, gson);
    }

    @Singleton @Provides MarkupProcessor provideMarkupProcessor() {
        return new MarkupProcessor(applicationContext.getString(R.string.format_markup_list_deliminator));
    }

    @Singleton @Provides Gson provideGson(@NonNull MarkupProcessor markupProcessor) {
        return createConfiguredGson(markupProcessor);
    }

    @Singleton @Provides Cache provideCache(@NonNull @ApiAppContext Context context) {
        File cacheDirectory = new File(context.getExternalCacheDir(), Constants.HTTP_CACHE_NAME);
        return new Cache(cacheDirectory, Constants.HTTP_CACHE_SIZE);
    }

    @Singleton @Provides OkHttpClient provideHttpClient(@NonNull Cache cache) {
        OkHttpClient client = new OkHttpClient();
        client.setCache(cache);
        client.setConnectTimeout(Constants.HTTP_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        client.setReadTimeout(Constants.HTTP_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        return client;
    }

    @Singleton @Provides RestAdapter provideRestAdapter(@NonNull Gson gson,
                                                        @NonNull OkHttpClient httpClient,
                                                        @NonNull ApiEndpoint endpoint,
                                                        @NonNull ApiSessionManager sessionManager) {
        RestAdapter.Builder builder = new RestAdapter.Builder();
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
            request.addHeader(ApiService.HEADER_CLIENT_VERSION, BuildConfig.VERSION_NAME);
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
