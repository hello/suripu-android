package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class SleepDurations extends ApiResponse {
    @SerializedName("durations")
    private List<Duration> durations;


    public List<Duration> getDurations() {
        return durations;
    }
}
