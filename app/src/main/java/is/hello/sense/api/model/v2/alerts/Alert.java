package is.hello.sense.api.model.v2.alerts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.Constants;

/**
 * Object representing an alert
 title (string, optional) - localized title of the alert
 body (string, required) - localized body of the alert
 category (enum, required) - a category of the alert that can drive user
 actions
 */

public class Alert extends ApiResponse {

    @SerializedName("title")
    private final String title;

    @SerializedName("body")
    private final String body;

    @SerializedName("category")
    private final Category category;

    public Alert(@Nullable final String title,
                 @NonNull final String body,
                 @NonNull final Category category) {
        this.title = title;
        this.body = body;
        this.category = category;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getBody() {
        return body;
    }

    @NonNull
    public Category getCategory() {
        return category;
    }

    public boolean isValid() {
        return !Constants.EMPTY_STRING.equals(body);
    }

    public static Alert NewEmptyInstance() {
        return new Alert(null,
                         Constants.EMPTY_STRING,
                         Category.UNKNOWN);
    }

    public interface ActionHandler {
        void unMuteSense();
    }
}
