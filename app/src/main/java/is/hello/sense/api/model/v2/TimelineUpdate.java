package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;

public final class TimelineUpdate extends ApiResponse {
    @JsonProperty("event_timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private DateTime timestamp;

    @JsonProperty("event_type")
    private TimelineEvent.Type eventType;

    @JsonProperty("new_event_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.TIME_FORMAT)
    private LocalTime newTime;


    //region Creation

    public static TimelineUpdate amendTime(@NonNull TimelineEvent event, @NonNull LocalTime newTime) {
        return new TimelineUpdate(event.getRawTimestamp(), event.getType(), newTime);
    }

    public static TimelineUpdate from(@NonNull TimelineEvent event) {
        return new TimelineUpdate(event.getRawTimestamp(), event.getType(), null);
    }

    public TimelineUpdate() {

    }

    private TimelineUpdate(@NonNull DateTime timestamp,
                           @NonNull TimelineEvent.Type eventType,
                           @Nullable LocalTime newTime) {
        this.timestamp = timestamp;
        this.newTime = newTime;
        this.eventType = eventType;
    }

    //endregion


    //region Attributes

    public @NonNull DateTime getTimestamp() {
        return timestamp;
    }

    public @NonNull TimelineEvent.Type getEventType() {
        return eventType;
    }

    public @Nullable LocalTime getNewTime() {
        return newTime;
    }


    @Override
    public String toString() {
        return "TimelineUpdate{" +
                "timestamp=" + timestamp +
                ", newTime=" + newTime +
                ", type=" + eventType +
                '}';
    }

    //endregion
}
