package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StoreReview extends ApiResponse {
    @SerializedName("like")
    public final Like like;

    @SerializedName("review")
    public final boolean review;


    public StoreReview(@NonNull Like like, boolean review) {
        this.like = like;
        this.review = review;
    }

    @Override
    public String toString() {
        return "StoreReview{" +
                "like=" + like +
                ", review=" + review +
                '}';
    }


    public enum Like {
        YES,
        NO,
        MAYBE,
        OTHER,
    }
}
