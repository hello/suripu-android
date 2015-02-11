package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import is.hello.sense.api.ApiService;

public class Feedback extends ApiResponse {
    @JsonProperty("event_type")
    private TimelineSegment.EventType eventType;

    @JsonProperty("day")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private LocalDate day;

    @JsonProperty("ts")
    private DateTime timestamp;


    public Feedback() {

    }


    public TimelineSegment.EventType getEventType() {
        return eventType;
    }

    public void setEventType(TimelineSegment.EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public String toString() {
        return "Feedback{" +
                "eventType=" + eventType +
                ", day=" + day +
                ", timestamp=" + timestamp +
                '}';
    }
}
