package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.Constants;
import is.hello.sense.util.IListObject;

public class SleepDurations extends ApiResponse implements IListObject, SleepSoundsPlayerView.ISleepSoundsPlayerRowItem {
    @SerializedName("durations")
    private List<Duration> durations;


    public List<Duration> getDurations() {
        return durations;
    }

    public Duration getDurationWithId(final int id) {
        if (id == Constants.NONE) {
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
    public List<? extends IListItem> getListItems() {
        return this.durations;
    }

    @Override
    public int getLabelRes() {
        return R.string.sleep_sounds_duration_label;
    }

    @Override
    public int getImageRes() {
        return R.drawable.icon_clock_24;
    }

    @Override
    public IListObject getListObject() {
        return this;
    }
}
