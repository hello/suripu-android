package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.joda.time.Instant;

import is.hello.sense.api.model.ApiResponse;

/**
 * Created by jimmy on 4/8/16.
 */
public class SleepSoundActionPlay extends ApiResponse {

    @SerializedName("sound")
    private final int soundId;

    @SerializedName("duration")
    private final int durationId;

    @SerializedName("order")
    private final Long order;

    @SerializedName("volume_percent")
    private final int volume;

    public SleepSoundActionPlay(final int soundId,
                                final int durationId,
                                final int volume) {
        this.order = Instant.now().getMillis(); // needs to be always incrementing
        this.soundId = soundId;
        this.durationId = durationId;
        this.volume = volume;
    }

    public int getSoundId() {
        return soundId;
    }

    public int getDurationId() {
        return durationId;
    }

    public Long getOrder() {
        return order;
    }

    public Integer getVolume() {
        return volume;
    }
}
