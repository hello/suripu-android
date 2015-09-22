package is.hello.sense.api.model;

import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

public class AppUnreadStats extends ApiResponse {
    @SerializedName("has_unread_insights")
    private boolean hasUnreadInsights;

    @SerializedName("has_unanswered_questions")
    private boolean hasUnansweredQuestions;

    @VisibleForTesting
    public AppUnreadStats(boolean hasUnreadInsights,
                          boolean hasUnansweredQuestions) {
        this.hasUnreadInsights = hasUnreadInsights;
        this.hasUnansweredQuestions = hasUnansweredQuestions;
    }

    public boolean hasUnreadInsights() {
        return hasUnreadInsights;
    }

    public boolean hasUnansweredQuestions() {
        return hasUnansweredQuestions;
    }


    @Override
    public String toString() {
        return "AppUnreadStats{" +
                "hasUnreadInsights=" + hasUnreadInsights +
                ", hasUnansweredQuestions=" + hasUnansweredQuestions +
                '}';
    }
}
