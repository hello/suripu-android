package is.hello.sense.api.model;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.api.model.v2.ImageUrl;
import is.hello.sense.util.markup.text.MarkupString;

public class Insight extends ApiResponse {
    public static final String CATEGORY_GENERIC = "GENERIC";
    public static final String CATEGORY_IN_APP_ERROR = Insight.class.getName() + ".CATEGORY_IN_APP_ERROR";

    @SerializedName("account_id")
    private long accountId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private MarkupString message;

    @SerializedName("timestamp")
    private DateTime created;

    @SerializedName("category")
    private String category;

    @SerializedName("info_preview")
    private String infoPreview;

    @SerializedName("image")
    private ImageUrl imageUrl;


    public static Insight createError(@NonNull String message) {
        Insight insight = new Insight();
        insight.message = new MarkupString(message);
        insight.category = CATEGORY_IN_APP_ERROR;
        return insight;
    }

    @VisibleForTesting
    public static Insight create(long accountId,
                                 String title,
                                 MarkupString message,
                                 DateTime created,
                                 String category,
                                 String infoPreview) {
        Insight insight = new Insight();
        insight.accountId = accountId;
        insight.title = title;
        insight.message = message;
        insight.created = created;
        insight.category = category;
        insight.infoPreview = infoPreview;
        insight.imageUrl = null;
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

    public String getCategory() {
        return category;
    }

    public String getFormattedInsightCategory(){
        return category.replaceAll("_", " ").toUpperCase();
    }

    public String getInfoPreview() {
        return infoPreview;
    }

    public String getImageUrl(@NonNull Resources resources) {
        if (imageUrl == null) {
            return null;
        } else {
            return imageUrl.getUrl(resources);
        }
    }

    public boolean isError() {
        return CATEGORY_IN_APP_ERROR.equalsIgnoreCase(category);
    }

    public boolean hasInfo() {
        return !CATEGORY_GENERIC.equalsIgnoreCase(category);
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
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }




}
