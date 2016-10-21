package is.hello.sense.api.model.v2.voice;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class SenseVoiceSettings extends ApiResponse {
    @SerializedName("volume")
    private int volume;

    @SerializedName("muted")
    private boolean isMuted;

    @SerializedName("is_primary_user")
    private boolean isPrimaryUser;

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

    @Override
    public String toString() {
        return "SenseVoiceSettings{" +
                "volume=" + volume +
                ", isMuted=" + isMuted +
                ", isPrimaryUser=" + isPrimaryUser +
                '}';
    }
}
