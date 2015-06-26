package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import is.hello.sense.api.ApiService;

public class Feedback extends ApiResponse {
    @JsonProperty("event_type")
    private TimelineSegment.EventType eventType;

    @JsonProperty("date_of_night")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private LocalDate night;

    @JsonProperty("old_time_of_event")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.TIME_FORMAT)
    private LocalTime oldTime;

    @JsonProperty("new_time_of_event")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.TIME_FORMAT)
    private LocalTime newTime;


    public TimelineSegment.EventType getEventType() {
        return eventType;
    }

    public void setEventType(TimelineSegment.EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDate getNight() {
        return night;
    }

    public void setNight(LocalDate night) {
        this.night = night;
    }


    public LocalTime getOldTime() {
        return oldTime;
    }

    public void setOldTime(LocalTime oldTime) {
        this.oldTime = oldTime;
    }

    public LocalTime getNewTime() {
        return newTime;
    }

    public void setNewTime(LocalTime newTime) {
        this.newTime = newTime;
    }


    @Override
    public String toString() {
        return "Feedback{" +
                "oldTime=" + oldTime +
                ", newTime=" + newTime +
                ", eventType=" + eventType +
                ", night=" + night +
                '}';
    }
}
