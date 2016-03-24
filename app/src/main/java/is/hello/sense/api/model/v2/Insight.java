package is.hello.sense.api.model.v2;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.markup.text.MarkupString;

public class Insight extends ApiResponse {
    public static final String CATEGORY_IN_APP_ERROR = Insight.class.getName() + ".CATEGORY_IN_APP_ERROR";

    private enum Type implements Enums.FromString {
        DEFAULT,
        BASIC;

        public static Type fromString(@Nullable String string) {
            return Enums.fromString(string, values(), DEFAULT);
        }
    }

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

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("image")
    private MultiDensityImage image;

    @SerializedName("insight_type")
    private Type insightType;


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
                                 String categoryName) {
        final Insight insight = new Insight();
        insight.accountId = accountId;
        insight.title = title;
        insight.message = message;
        insight.created = created;
        insight.category = category;
        insight.categoryName = categoryName;
        insight.image = null;
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

    public String getCategoryName() {
        return categoryName;
    }

    public String getImageUrl(@NonNull Resources resources) {
        if (image == null) {
            return null;
        } else {
            return image.getUrl(resources);
        }
    }

    public boolean isError() {
        return CATEGORY_IN_APP_ERROR.equalsIgnoreCase(category);
    }

    public boolean shouldDisplaySummary() {
        return (insightType != Type.BASIC);
    }

    @Override
    public String toString() {
        return "Insight{" +
                "accountId=" + accountId +
                ", title='" + title + '\'' +
                ", message=" + message +
                ", created=" + created +
                ", category='" + category + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", image=" + image +
                '}';
    }
}
