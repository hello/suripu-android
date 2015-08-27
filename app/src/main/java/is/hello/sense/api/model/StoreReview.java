package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StoreReview extends ApiResponse {
    @SerializedName("like")
    public final Feedback feedback;

    @SerializedName("review")
    public final boolean review;


    public StoreReview(@NonNull Feedback feedback, boolean review) {
        this.feedback = feedback;
        this.review = review;
    }

    @Override
    public String toString() {
        return "StoreReview{" +
                "like=" + feedback +
                ", review=" + review +
                '}';
    }


    public enum Feedback {
        YES,
        NO,
        HELP,
    }
}
