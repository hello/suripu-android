package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class SupportTopic extends ApiResponse {
    @SerializedName("topic")
    public final String topic;

    @SerializedName("display_name")
    public final String displayName;

    public SupportTopic(@NonNull String topic,
                        @NonNull String displayName) {
        this.topic = topic;
        this.displayName = displayName;
    }
}
