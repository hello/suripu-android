package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
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
    private Category category;


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

    public Category getCategory() {
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


    public static enum Category {
        GENERIC,
        SLEEP_HYGIENE,
        LIGHT,
        SOUND,
        TEMPERATURE,
        HUMIDITY,
        AIR_QUALITY,
        SLEEP_DURATION,
        SLEEP_TIME,
        WAKEUP_TIME,
        WORKOUT,
        CAFFEINE,
        ALCOHOL,
        DAYTIME_SLEEPINESS,
        DIET,
        SLEEP_QUALITY;

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static Category fromString(@NonNull String value) {
                return Enums.fromString(value, values(), GENERIC);
        }
    }
}
