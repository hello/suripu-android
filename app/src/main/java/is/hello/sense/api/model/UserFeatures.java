package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

public class UserFeatures extends ApiResponse {
    @SerializedName("VOICE")
    public final boolean voice;

    public UserFeatures(final boolean voice){
        this.voice = voice;
    }
}
