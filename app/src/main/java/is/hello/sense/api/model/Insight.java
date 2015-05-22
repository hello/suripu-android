package is.hello.sense.api.model;

import android.support.annotation.NonNull;

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

    @JsonProperty("info_preview")
    private String infoPreview;


    public static Insight createError(@NonNull String message) {
        Insight insight = new Insight();
        insight.message = message;
        insight.category = InsightCategory.IN_APP_ERROR;
        return insight;
    }


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

    public String getInfoPreview() {
        return infoPreview;
    }

    @Override
    public String toString() {
        return "Insight{" +
                "accountId=" + accountId +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", created=" + created +
                ", category=" + category +
                ", infoPreview='" + infoPreview + '\'' +
                '}';
    }


}
