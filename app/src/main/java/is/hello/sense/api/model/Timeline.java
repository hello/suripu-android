package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timeline extends ApiResponse {
    @JsonProperty("score")
    private int score;

    @JsonProperty("message")
    private String message;

    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-M-d")
    private DateTime date;

    @JsonProperty("segments")
    @JsonDeserialize(contentAs = TimelineSegment.class)
    private List<TimelineSegment> segments;


    public int getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    public DateTime getDate() {
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
