package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public class TimelineSegment extends ApiResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("duration")
    private long duration;

    @JsonProperty("message")
    private String message;

    @JsonProperty("sensors")
    @JsonDeserialize(contentAs = TimelineSensor.class)
    private List<TimelineSensor> sensors;

    @JsonProperty("sleep_depth")
    private long sleepDepth;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("offset_millis")
    private long offset;


    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public String getMessage() {
        return message;
    }

    public List<TimelineSensor> getSensors() {
        return sensors;
    }

    public long getSleepDepth() {
        return sleepDepth;
    }

    public String getEventType() {
        return eventType;
    }

    public long getOffset() {
        return offset;
    }


    @Override
    public String toString() {
        return "TimelineSegment{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                ", sleepDepth=" + sleepDepth +
                ", offset=" + offset +
                '}';
    }
}
