package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public class Timeline extends ApiResponse {
    @JsonProperty("score")
    private long score;

    @JsonProperty("message")
    private String message;

    @JsonProperty("date")
    private String date; //mm-dd-yyyy

    @JsonProperty("segments")
    @JsonDeserialize(contentAs = TimelineSegment.class)
    private List<TimelineSegment> segments;


    public long getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public List<TimelineSegment> getSegments() {
        return segments;
    }


    @Override
    public String toString() {
        return "Timeline{" +
                "score=" + score +
                ", message='" + message + '\'' +
                ", date='" + date + '\'' +
                ", segments=" + segments +
                '}';
    }
}
