package is.hello.sense.api;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineBuilder;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.TimelineEventBuilder;
import is.hello.sense.util.markup.text.MarkupString;
import retrofit.http.Body;
import retrofit.http.Path;
import rx.Observable;

import static is.hello.sense.api.TestApiService.safeJust;

public class TestTimelineService implements TimelineService {
    @Override
    public Observable<Timeline> timelineForDate(@NonNull @Path("date") String date) {
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormat.forPattern(ApiService.DATE_FORMAT));
        return safeJust(new TimelineBuilder()
                                .setDate(dateTime)
                                .setScore(90, ScoreCondition.IDEAL)
                                .setMessage(new MarkupString("This is *just* a test."))
                                .build());
    }



    @Override
    public Observable<VoidResponse> verifyTimelineEvent(@NonNull @Path("date") String date,
                                                        @NonNull @Path("type") TimelineEvent.Type type,
                                                        @Path("timestamp") long timestamp,
                                                        @NonNull @Body String stupidOkHttp) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<Timeline> amendTimelineEventTime(@NonNull @Path("date") String date,
                                                       @NonNull @Path("type") TimelineEvent.Type type,
                                                       @Path("timestamp") long timestamp,
                                                       @NonNull @Body TimelineEvent.TimeAmendment amendment) {
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormat.forPattern(ApiService.DATE_FORMAT));
        return safeJust(new TimelineBuilder()
                                .setDate(dateTime)
                                .setScore(90, ScoreCondition.IDEAL)
                                .setMessage(new MarkupString("This is *just* a test."))
                                .addEvent(new TimelineEventBuilder()
                                                  .setType(type)
                                                  .setShiftedTimestamp(new DateTime(timestamp,
                                                                                    DateTimeZone.getDefault())
                                                                               .withTime(amendment.newTime))
                                                  .build())
                                .build());
    }

    @Override
    public Observable<Timeline> deleteTimelineEvent(@NonNull @Path("date") String date,
                                                    @NonNull @Path("type") TimelineEvent.Type type,
                                                    @Path("timestamp") long timestamp) {
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormat.forPattern(ApiService.DATE_FORMAT));
        return safeJust(new TimelineBuilder()
                                .setDate(dateTime)
                                .setScore(90, ScoreCondition.IDEAL)
                                .setMessage(new MarkupString("This is *just* a test."))
                                .addEvent(new TimelineEventBuilder()
                                                  .setType(type)
                                                  .setShiftedTimestamp(new DateTime(timestamp,
                                                                                    DateTimeZone.getDefault()))
                                                  .build())
                                .build());
    }
}
