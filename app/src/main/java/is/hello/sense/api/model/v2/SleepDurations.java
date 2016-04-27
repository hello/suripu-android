package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.ListObject;

public class SleepDurations extends ApiResponse implements ListObject {
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
    public List<? extends ListItem> getListItems() {
        return this.durations;
    }
}
