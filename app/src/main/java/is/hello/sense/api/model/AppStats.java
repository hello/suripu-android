package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class AppStats extends ApiResponse {
    @SerializedName("insights_last_viewed")
    public final @Nullable DateTime insightsLastViewed;

    @SerializedName("questions_last_viewed")
    public final @Nullable DateTime questionsLastViewed;

    public static AppStats withLastViewedForNow() {
        return new AppStats(DateTime.now(DateTimeZone.getDefault()),
                            DateTime.now(DateTimeZone.UTC));
    }

    public AppStats(@Nullable DateTime insightsLastViewed,
                    @Nullable DateTime questionsLastViewed) {
        this.insightsLastViewed = insightsLastViewed;
        this.questionsLastViewed = questionsLastViewed;
    }


    @Override
    public String toString() {
        return "AppStats{" +
                "insightsLastViewed=" + insightsLastViewed +
                ", questionsLastViewed=" + questionsLastViewed +
                '}';
    }
}
