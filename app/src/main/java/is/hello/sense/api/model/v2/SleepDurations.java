package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.IListObject;

public class SleepDurations extends ApiResponse
        implements IListObject<Duration>, SleepSoundsPlayerView.ISleepSoundsPlayerRowItem<Duration> {
    @SerializedName("durations")
    private List<Duration> durations;


    public List<Duration> getDurations() {
        return durations;
    }

    public boolean hasDuration(final @Nullable String durationName) {
        if (durationName == null) {
            return false;
        }
        for (final Duration duration : durations) {
            if (duration.getName().equals(durationName)) {
                return true;
            }
        }
        return false;
    }

    public Duration getDurationWithId(final int id) {
        if (id == -1) {
            return null;
        }
        for (final Duration duration : durations) {
            if (duration.getId() == id) {
                return duration;
            }
        }
        return null;
    }

    @Override
    public List<Duration> getListItems() {
        return this.durations;
    }

    @Override
    public int getLabelRes() {
        return R.string.sleep_sounds_duration_label;
    }

    @Override
    public int getImageRes() {
        return R.drawable.sounds_duration_icon;
    }

    @Override
    public IListObject<Duration> getListObject() {
        return this;
    }
}
