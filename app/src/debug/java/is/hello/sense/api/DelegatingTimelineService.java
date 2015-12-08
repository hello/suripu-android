package is.hello.sense.api;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.util.Logger;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.PATCH;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

public class DelegatingTimelineService implements TimelineService {
    private final TimelineService originalDelegate;
    private TimelineService delegate;

    public DelegatingTimelineService(@NonNull TimelineService delegate) {
        this.originalDelegate = delegate;
        this.delegate = delegate;
    }

    public void setDelegate(@NonNull TimelineService delegate) {
        Logger.debug(getClass().getSimpleName(), "setDelegate(" + delegate + ")");

        this.delegate = delegate;
    }

    public @NonNull TimelineService getDelegate() {
        return delegate;
    }

    public void reset() {
        setDelegate(originalDelegate);
    }

    @Override
    @GET("/v2/timeline/{date}")
    public Observable<Timeline> timelineForDate(@NonNull @Path("date") String date) {
        return delegate.timelineForDate(date);
    }

    @Override
    @PATCH("/v2/timeline/{date}/events/{type}/{timestamp}")
    public Observable<Timeline> amendTimelineEventTime(@NonNull @Path("date") String date,
                                                       @NonNull @Path("type") TimelineEvent.Type type,
                                                       @Path("timestamp") long timestamp,
                                                       @NonNull @Body TimelineEvent.TimeAmendment amendment) {
        return delegate.amendTimelineEventTime(date, type, timestamp, amendment);
    }

    @Override
    @DELETE("/v2/timeline/{date}/events/{type}/{timestamp}")
    public Observable<Timeline> deleteTimelineEvent(@NonNull @Path("date") String date,
                                                    @NonNull @Path("type") TimelineEvent.Type type,
                                                    @Path("timestamp") long timestamp) {
        return delegate.deleteTimelineEvent(date, type, timestamp);
    }

    @Override
    @PUT("/v2/timeline/{date}/events/{type}/{timestamp}")
    @Headers("Content-Type: application/json")
    public Observable<VoidResponse> verifyTimelineEvent(@NonNull @Path("date") String date,
                                                        @NonNull @Path("type") TimelineEvent.Type type,
                                                        @Path("timestamp") long timestamp,
                                                        @NonNull @Body String stupidOkHttp) {
        return delegate.verifyTimelineEvent(date, type, timestamp, stupidOkHttp);
    }
}
