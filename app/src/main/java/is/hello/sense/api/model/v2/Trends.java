package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

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
        return availableTimeScales.toArray(new TimeScale[availableTimeScales.size()]);
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
        NONE(R.string.empty),
        LAST_WEEK(R.string.trend_time_scale_week),
        LAST_MONTH(R.string.trend_time_scale_month),
        LAST_3_MONTHS(R.string.trend_time_scale_quarter);

        public final
        @StringRes
        int titleRes;

        TimeScale(@StringRes int titleRes) {
            this.titleRes = titleRes;
        }

        public static TimeScale fromString(@NonNull String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }
}
