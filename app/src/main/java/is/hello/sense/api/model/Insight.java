package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

public class Insight extends ApiResponse {
    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("created_utc")
    private DateTime created;


    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public DateTime getCreated() {
        return created;
    }


    @Override
    public String toString() {
        return "Insight{" +
                "created=" + created +
                ", message='" + message + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
