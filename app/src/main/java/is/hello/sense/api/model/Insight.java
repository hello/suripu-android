package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

public class Insight extends ApiResponse {
    @JsonProperty("account_id")
    private long accountId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private DateTime created;

    @JsonProperty("category")
    private InsightCategory category;


    public long getAccountId() {
        return accountId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public DateTime getCreated() {
        return created;
    }

    public InsightCategory getCategory() {
        return category;
    }


    @Override
    public String toString() {
        return "Insight{" +
                "accountId=" + accountId +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", created=" + created +
                ", category=" + category +
                '}';
    }


}
