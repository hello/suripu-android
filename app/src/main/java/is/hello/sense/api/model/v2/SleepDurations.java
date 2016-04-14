package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class SleepDurations extends ApiResponse {
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

    public Duration getDurationWithName(final @Nullable String name) {
        if (name == null) {
            return null;
        }
        for (final Duration duration: durations) {
            if (duration.getName().equals(name)) {
                return duration;
            }
        }
        return null;
    }
}
