package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.IListObject;

public class SleepSoundStatus extends ApiResponse
        implements IListObject<Volume>, SleepSoundsPlayerView.ISleepSoundsPlayerRowItem<Volume> {

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
        final ArrayList<Volume> volumes = new ArrayList<>();
        volumes.add(Volume.Low);
        volumes.add(Volume.Medium);
        volumes.add(Volume.High);
        return volumes;
    }

    @Override
    public List<Volume> getListItems() {
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
    public IListObject<Volume> getListObject() {
        return this;
    }

}
