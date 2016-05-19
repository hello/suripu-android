package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.IListObject;

public class SleepSoundStatus extends ApiResponse implements IListObject, SleepSoundsPlayerView.ISleepSoundsPlayerRowItem {
    private static final int VolumeAccuracyOffset = 5;

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

    public Volume getVolumeWithValue(final int value) {
        return Volume.fromInt(value);
    }

    public ArrayList<Volume> getVolumes() {
        ArrayList<Volume> volumes = new ArrayList<>();
        volumes.add(Volume.Low);
        volumes.add(Volume.Medium);
        volumes.add(Volume.High);
        return volumes;
    }

    @Override
    public List<? extends IListItem> getListItems() {
        return getVolumes();
    }

    @Override
    public int getLabelRes() {
        return R.string.sleep_sounds_volume_label;
    }

    @Override
    public int getImageRes() {
        return R.drawable.sounds_volume_icon;
    }

    @Override
    public IListObject getListObject() {
        return this;
    }


    public enum Volume implements Enums.FromString, IListItem {
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
            if (value == null) {
                return None;
            }
            for (Volume volume : values()) {
                if (volume.volume <= value + VolumeAccuracyOffset && volume.volume >= value - VolumeAccuracyOffset) {
                    return volume;
                }
            }
            return None;
        }

        @Override
        public int getId() {
            return getVolume();
        }

        @Override
        public String getName() {
            return this.toString();
        }

        @Override
        public String getPreviewUrl() {
            return null;
        }
    }
}
