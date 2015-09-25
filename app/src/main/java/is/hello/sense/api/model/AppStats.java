package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

public class AppStats extends ApiResponse {
    @SerializedName("insights_last_viewed")
    private @Nullable DateTime insightsLastViewed;


    public @Nullable DateTime getInsightsLastViewed() {
        return insightsLastViewed;
    }

    public void setInsightsLastViewed(@Nullable DateTime insightsLastViewed) {
        this.insightsLastViewed = insightsLastViewed;
    }


    @Override
    public String toString() {
        return "AppStats{" +
                "insightsLastViewed=" + insightsLastViewed +
                '}';
    }
}
