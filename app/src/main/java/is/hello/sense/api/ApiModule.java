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
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

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
import is.hello.buruberi.util.Rx;
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
import is.hello.sense.functional.Functions;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.markup.MarkupDeserializer;
import is.hello.sense.util.markup.MarkupProcessor;
import is.hello.sense.util.markup.text.MarkupString;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import rx.Observable;
import rx.schedulers.Schedulers;

@Module(
    library = true
)
@SuppressWarnings("UnusedDeclaration")
public class ApiModule {
    private final Context applicationContext;

    public static Gson createConfiguredGson(@NonNull MarkupProcessor markupProcessor) {
        final GsonBuilder builder = new GsonBuilder();
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

    @Provides Observable<Cache> provideCache(@NonNull @ApiAppContext Context context) {
        final Observable.OnSubscribe<Cache> onSubscribe = subscriber -> {
            // Context#getExternalCacheDir() causes synchronous file system access.
            final File cacheDirectory = new File(context.getExternalCacheDir(),
                                                 Constants.HTTP_CACHE_NAME);
            final Cache cache = new Cache(cacheDirectory, Constants.HTTP_CACHE_SIZE);

            subscriber.onNext(cache);
            subscriber.onCompleted();

            Logger.debug(getClass().getSimpleName(), "Cache ready.");
        };
        return Observable.create(onSubscribe)
                         .subscribeOn(Schedulers.io())
                         .observeOn(Rx.mainThreadScheduler());
    }

    @Singleton @Provides OkHttpClient provideHttpClient(@NonNull Observable<Cache> cache) {
        final OkHttpClient client = new OkHttpClient();
        cache.subscribe(client::setCache, Functions.LOG_ERROR);
        client.setConnectTimeout(Constants.HTTP_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        client.setReadTimeout(Constants.HTTP_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        return client;
    }

    @Singleton @Provides LruCache providePicassoCache(@NonNull @ApiAppContext Context context) {
        return new LruCache(context);
    }

    @Singleton @Provides Picasso providePicasso(@NonNull @ApiAppContext Context context,
                                                @NonNull OkHttpClient client,
                                                @NonNull LruCache cache) {
        final Picasso.Builder builder = new Picasso.Builder(context);
        builder.downloader(new OkHttpDownloader(client));
        builder.memoryCache(cache);
        return builder.build();
    }

    @Singleton @Provides RestAdapter provideRestAdapter(@NonNull Gson gson,
                                                        @NonNull OkHttpClient httpClient,
                                                        @NonNull ApiEndpoint endpoint,
                                                        @NonNull ApiSessionManager sessionManager) {
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

    @Singleton @Provides TimelineService provideTimelineService(@NonNull RestAdapter adapter) {
        return adapter.create(TimelineService.class);
    }
}
