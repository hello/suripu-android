package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

/**
 * Created by jimmy on 4/8/16.
 */
public class SleepSoundStatus extends ApiResponse {

    @SerializedName("playing")
    private Boolean playing;

    @SerializedName("sound")
    private Sound sound;

    @SerializedName("duration")
    private Duration duration;

    @SerializedName("volume_percent")
    private Integer volume;

    public Boolean isPlaying() {
        return playing;
    }

    public Sound getSound() {
        return sound;
    }

    public Duration getDuration() {
        return duration;
    }

    public Integer getVolume() {
        return volume;
    }
}
