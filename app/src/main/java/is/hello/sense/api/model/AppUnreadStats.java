package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

public class AppUnreadStats extends ApiResponse {
    @SerializedName("has_unread_insights")
    private boolean hasUnreadInsights;

    @SerializedName("has_unanswered_questions")
    private boolean hasUnansweredQuestions;


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
