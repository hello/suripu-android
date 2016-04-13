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
    private final Long soundId;

    @SerializedName("duration")
    private final Long durationId;

    @SerializedName("order")
    private final Long order;

    @SerializedName("volume_percent")
    private final Integer volume;

    public SleepSoundActionPlay(@NonNull final Long soundId,
                                @NonNull final Long durationId,
                                @NonNull final Integer volume) {
        this.order = Instant.now().getMillis(); // needs to be always incrementing
        this.soundId = soundId;
        this.durationId = durationId;
        this.volume = volume;
    }

    public Long getSoundId() {
        return soundId;
    }

    public Long getDurationId() {
        return durationId;
    }

    public Long getOrder() {
        return order;
    }

    public Integer getVolume() {
        return volume;
    }
}
