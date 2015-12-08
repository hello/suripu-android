package is.hello.sense.api;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.PATCH;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

public interface TimelineService {
    @GET("/v2/timeline/{date}")
    Observable<Timeline> timelineForDate(@NonNull @Path("date") String date);

    @PATCH("/v2/timeline/{date}/events/{type}/{timestamp}")
    Observable<Timeline> amendTimelineEventTime(@NonNull @Path("date") String date,
                                                @NonNull @Path("type") TimelineEvent.Type type,
                                                @Path("timestamp") long timestamp,
                                                @NonNull @Body TimelineEvent.TimeAmendment amendment);

    @DELETE("/v2/timeline/{date}/events/{type}/{timestamp}")
    Observable<Timeline> deleteTimelineEvent(@NonNull @Path("date") String date,
                                             @NonNull @Path("type") TimelineEvent.Type type,
                                             @Path("timestamp") long timestamp);

    @PUT("/v2/timeline/{date}/events/{type}/{timestamp}")
    @Headers("Content-Type: application/json")
    Observable<VoidResponse> verifyTimelineEvent(@NonNull @Path("date") String date,
                                                 @NonNull @Path("type") TimelineEvent.Type type,
                                                 @Path("timestamp") long timestamp,
                                                 @NonNull @Body String stupidOkHttp);
}
