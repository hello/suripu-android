package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

/**
 * Created by jimmy on 4/8/16.
 */
public class SleepSoundsState extends ApiResponse {

    @SerializedName("availableDurations")
    private SleepDurations durations;

    @SerializedName("availableSounds")
    private SleepSounds sounds;

    @SerializedName("status")
    private SleepSoundStatus status;

    public SleepDurations getDurations() {
        return durations;
    }

    public SleepSounds getSounds() {
        return sounds;
    }

    public SleepSoundStatus getStatus() {
        return status;
    }
}
