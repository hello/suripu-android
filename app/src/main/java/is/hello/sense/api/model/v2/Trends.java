package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;

public class Trends extends ApiResponse {
    @SerializedName("available_time_scales")
    private List<TimeScale> availableTimeScales;

    @SerializedName("graphs")
    private List<Graph> graphs;

    @VisibleForTesting
    public Trends(@NonNull List<TimeScale> availableTimeScales,
                  @NonNull List<Graph> graphs) {
        this.availableTimeScales = availableTimeScales;
        this.graphs = graphs;
    }

    public List<TimeScale> getAvailableTimeScales() {
        return availableTimeScales;
    }

    public Object[] getAvailableTimeScaleTags() {
        Object[] tags = new Object[availableTimeScales.size()];
        int index = 0;
        for (TimeScale timeScale : availableTimeScales) {
            tags[index++] = (timeScale);
        }
        return tags;
    }

    public List<Integer> getAvailableTimeScaleStringResList() {
        List<Integer> stringResList = new ArrayList<>();
        for (TimeScale timeScale : availableTimeScales) {
            switch (timeScale) {
                case LAST_WEEK:
                    stringResList.add(R.string.trend_time_scale_week);
                    break;
                case LAST_MONTH:
                    stringResList.add(R.string.trend_time_scale_month);
                    break;
                case LAST_3_MONTHS:
                    stringResList.add(R.string.trend_time_scale_quarter);
                    break;
            }
        }
        return stringResList;
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

    public enum TimeScale implements Enums.FromString {
        NONE,
        LAST_WEEK,
        LAST_MONTH,
        LAST_3_MONTHS;

        public static TimeScale fromString(@NonNull String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }
}
