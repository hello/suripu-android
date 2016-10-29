package is.hello.sense.api.model.v2.voice;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
//todo fix equals method.
public class SenseVoiceSettings extends ApiResponse {

    /**
     * Difference tolerance in volume to be considered equal
     */
    @VisibleForTesting
    public static final int VOLUME_EQUALS_THRESHOLD = 2;

    public static final int TOTAL_VOLUME_LEVELS = 11;

    /**
     * Probably wont ever be used but just in case.
     */
    public static final int DEFAULT_START_VOLUME = 3;

    @SerializedName("volume")
    private Integer volume;

    @SerializedName("muted")
    private Boolean isMuted;

    @SerializedName("is_primary_user")
    private Boolean isPrimaryUser;
    public static SenseVoiceSettings newInstance(@NonNull final SenseVoiceSettings latestSettings) {
        return new SenseVoiceSettings(latestSettings.volume,
                                      latestSettings.isMuted,
                                      latestSettings.isPrimaryUser);
    }

    //todo confirm if other constructor is needed
    public SenseVoiceSettings(final Integer volume,
                              final Boolean isMuted,
                              final Boolean isPrimaryUser) {
        this.volume = volume;
        this.isMuted = isMuted;
        this.isPrimaryUser = isPrimaryUser;
    }

    public SenseVoiceSettings(final int volume,
                              final boolean isMuted,
                              final boolean isPrimaryUser) {
        this.volume = volume;
        this.isMuted = isMuted;
        this.isPrimaryUser = isPrimaryUser;
    }

    @Nullable
    public Integer getVolume() {
        return volume;
    }

    public void setVolume(final int volume) {
        this.volume = volume;
    }

    @Nullable
    public Boolean isMuted() {
        return isMuted;
    }

    public void setMuted(final boolean muted) {
        isMuted = muted;
    }

    @Nullable
    public Boolean isPrimaryUser() {
        return isPrimaryUser;
    }

    public void setPrimaryUser(final boolean primaryUser) {
        isPrimaryUser = primaryUser;
    }

    public boolean isWithinVolumeThreshold(final int otherVolume) {
        return Math.abs(volume - otherVolume) <= VOLUME_EQUALS_THRESHOLD;
    }

    /**
     * Weird equal case. Read code.
     * @param o
     * @return
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SenseVoiceSettings that = (SenseVoiceSettings) o;

        if (this.isMuted != null && that.isMuted != null) {
            if (isMuted != that.isMuted) {
                return false;
            }
        }
        if (this.volume != null && that.volume != null) {
            if (!this.isWithinVolumeThreshold(that.volume)) {
                return false;
            }
        }
        if (this.isPrimaryUser != null && that.isPrimaryUser != null) {
            if (isPrimaryUser != that.isPrimaryUser) {
                return false;
            }
        }
        return true;

    }

    @Override
    public int hashCode() {
        int result = volume;
        result = 31 * result + (isMuted ? 1 : 0);
        result = 31 * result + (isPrimaryUser ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SenseVoiceSettings{" +
                "volume=" + volume +
                ", isMuted=" + isMuted +
                ", isPrimaryUser=" + isPrimaryUser +
                '}';
    }
}
