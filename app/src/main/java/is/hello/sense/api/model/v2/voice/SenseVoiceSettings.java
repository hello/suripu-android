package is.hello.sense.api.model.v2.voice;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class SenseVoiceSettings extends ApiResponse {

    /**
     * Difference tolerance in volume to be considered equal
     */
    private static final int VOLUME_EQUALS_THRESHOLD = 5;

    public static final int TOTAL_VOLUME_LEVELS = 11;

    @SerializedName("volume")
    private int volume;

    @SerializedName("muted")
    private boolean isMuted;

    @SerializedName("is_primary_user")
    private boolean isPrimaryUser;

    public static SenseVoiceSettings newCopyOf(@NonNull final SenseVoiceSettings latestSettings) {
        return new SenseVoiceSettings(latestSettings.volume,
                                      latestSettings.isMuted,
                                      latestSettings.isPrimaryUser);
    }

    public SenseVoiceSettings(final int volume,
                              final boolean isMuted,
                              final boolean isPrimaryUser) {
        this.volume = volume;
        this.isMuted = isMuted;
        this.isPrimaryUser = isPrimaryUser;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(final int volume) {
        this.volume = volume;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(final boolean muted) {
        isMuted = muted;
    }

    public boolean isPrimaryUser() {
        return isPrimaryUser;
    }

    public void setPrimaryUser(final boolean primaryUser) {
        isPrimaryUser = primaryUser;
    }

    public boolean isWithinVolumeThreshold(final int otherVolume){
        return Math.abs(volume - otherVolume) <= VOLUME_EQUALS_THRESHOLD;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SenseVoiceSettings that = (SenseVoiceSettings) o;

        if (!this.isWithinVolumeThreshold(that.volume)) return false;
        if (isMuted != that.isMuted) return false;
        return isPrimaryUser == that.isPrimaryUser;

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
