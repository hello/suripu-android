package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;

public class Trends extends ApiResponse {
    @SerializedName("available_time_scales")
    private  List<TimeScale> availableTimeScales;

    @SerializedName("graphs")
    private List<Graph> graphs;

    public List<TimeScale> getAvailableTimeScales() {
        return availableTimeScales;
    }

    public List<Graph> getGraphs() {
        return graphs;
    }

    @Override
    public String toString() {
        return "Trend{" +
            "availableTimeScales=" + availableTimeScales.toString() +
            ", graphs='" + graphs.toString() + '\'' +
            '}';
    }

    public static Trends createErrorTrend(){
        Trends trend  = new Trends();
        trend.graphs = Graph.createErrorGraphs();
        return trend;
    }

    public enum TimeScale implements Enums.FromString {
        NONE,
        LAST_WEEK,
        LAST_MONTH,
        LAST_3_MONTHS;

        public static TimeScale fromString(@NonNull String string){
            return Enums.fromString(string, values(), NONE);
        }
    }
}
