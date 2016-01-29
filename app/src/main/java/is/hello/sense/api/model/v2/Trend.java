package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;

public class Trend extends ApiResponse {
    @VisibleForTesting
    @SerializedName("available_time_scales")
    private TimeScale[] availableTimeScales;

    @VisibleForTesting
    @SerializedName("graphs")
    private Graph[] graphs;

    public TimeScale[] getAvailableTimeScales() {
        return availableTimeScales;
    }

    public Graph[] getGraphs() {
        return graphs;
    }

    @Override
    public String toString() {
        return "Trend{" +
            "availableTimeScales=" + Arrays.toString(availableTimeScales) +
            ", graphs='" + Arrays.toString(graphs) + '\'' +
            '}';
    }

    public enum TimeScale implements Enums.FromString {
        none,
        last_week,
        last_month,
        last_3_months;

        public static TimeScale fromString(@NonNull String string){
            return Enums.fromString(string, values(), none);
        }
    }
}
