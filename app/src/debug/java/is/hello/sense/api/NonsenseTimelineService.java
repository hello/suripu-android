package is.hello.sense.api;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import java.io.InvalidObjectException;

import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.util.Logger;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.Path;
import rx.Observable;

public class NonsenseTimelineService implements TimelineService {
    private final TimelineService delegate;
    private final String host;

    public NonsenseTimelineService(@NonNull OkHttpClient client,
                                   @NonNull Gson gson,
                                   @NonNull String host) {
        final RestAdapter adapter = new RestAdapter.Builder()
                .setClient(new OkClient(client))
                .setConverter(new GsonConverter(gson))
                .setEndpoint(host)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setLog(Logger.RETROFIT_LOGGER)
                .build();
        this.delegate = adapter.create(TimelineService.class);
        this.host = host;
    }

    public @NonNull String getHost() {
        return host;
    }

    @Override
    public Observable<Timeline> timelineForDate(@NonNull @Path("date") String date) {
        return delegate.timelineForDate(date);
    }

    @Override
    public Observable<Timeline> amendTimelineEventTime(@NonNull @Path("date") String date,
                                                       @NonNull @Path("type") TimelineEvent.Type type,
                                                       @Path("timestamp") long timestamp,
                                                       @NonNull @Body TimelineEvent.TimeAmendment amendment) {
        return Observable.error(new InvalidObjectException("Amendment not supported in nonsense mode"));
    }

    @Override
    public Observable<Timeline> deleteTimelineEvent(@NonNull @Path("date") String date,
                                                    @NonNull @Path("type") TimelineEvent.Type type,
                                                    @Path("timestamp") long timestamp) {
        return Observable.error(new InvalidObjectException("Amendment not supported in nonsense mode"));
    }

    @Override
    public Observable<VoidResponse> verifyTimelineEvent(@NonNull @Path("date") String date,
                                                        @NonNull @Path("type") TimelineEvent.Type type,
                                                        @Path("timestamp") long timestamp,
                                                        @NonNull @Body String stupidOkHttp) {
        return Observable.error(new InvalidObjectException("Amendment not supported in nonsense mode"));
    }
}
