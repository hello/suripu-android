package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.util.markup.text.MarkupString;

public class Insight extends ApiResponse {
    @SerializedName("account_id")
    private long accountId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private MarkupString message;

    @SerializedName("timestamp")
    private DateTime created;

    @SerializedName("category")
    private InsightCategory category;

    @SerializedName("info_preview")
    private String infoPreview;


    public static Insight createError(@NonNull String message) {
        Insight insight = new Insight();
        insight.message = new MarkupString(message);
        insight.category = InsightCategory.IN_APP_ERROR;
        return insight;
    }

    @VisibleForTesting
    public static Insight create(long accountId,
                                 String title,
                                 MarkupString message,
                                 DateTime created,
                                 InsightCategory category,
                                 String infoPreview) {
        Insight insight = new Insight();
        insight.accountId = accountId;
        insight.title = title;
        insight.message = message;
        insight.created = created;
        insight.category = category;
        insight.infoPreview = infoPreview;
        return insight;
    }


    public long getAccountId() {
        return accountId;
    }

    public String getTitle() {
        return title;
    }

    public MarkupString getMessage() {
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
