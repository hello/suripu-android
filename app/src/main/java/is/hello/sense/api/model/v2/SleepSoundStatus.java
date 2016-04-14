package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.gson.Enums;
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

    public Volume getVolume() {
        return Volume.fromInt(volume);
    }

    public Volume getVolume(final @NonNull String name) {
        return Volume.fromString(name);
    }

    public enum Volume implements Enums.FromString {
        High(100),
        Medium(50),
        Low(25),
        None(0);

        final int volume;

        Volume(int volume) {
            this.volume = volume;
        }

        public int getVolume() {
            return volume;
        }

        public static Volume fromString(@NonNull String string) {
            return Enums.fromString(string, values(), None);
        }

        public static Volume fromInt(@Nullable Integer value) {
            if (value == null){
                return None;
            }
            for (Volume volume : values()) {
                if (volume.volume == value) {
                    return volume;
                }
            }
            return None;
        }
    }
}
