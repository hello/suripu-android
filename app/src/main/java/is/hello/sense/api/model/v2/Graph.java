package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;

public class Graph extends ApiResponse {
    @VisibleForTesting
    @SerializedName("time_scale")
    private Trend.TimeScale timeScale;

    @VisibleForTesting
    @SerializedName("title")
    private String title;

    @VisibleForTesting
    @SerializedName("data_type")
    private DataType dataType;

    @VisibleForTesting
    @SerializedName("graph_type")
    private GraphType graphType;

    @VisibleForTesting
    @SerializedName("min_value")
    private int minValue;

    @VisibleForTesting
    @SerializedName("max_value")
    private int maxValue;

    @VisibleForTesting
    @SerializedName("sections")
    @NonNull
    private GraphSection[] sections;

    @VisibleForTesting
    @SerializedName("condition_ranges")
    @Nullable
    private ConditionRange[] conditionRanges;

    @VisibleForTesting
    @SerializedName("annotations")
    @Nullable
    private Annotation[] annotations;

    public Trend.TimeScale getTimeScale() {
        return timeScale;
    }

    public String getTitle() {
        return title;
    }

    public DataType getDataType() {
        return dataType;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public GraphSection[] getSections() {
        return sections;
    }

    @Nullable
    public ConditionRange[] getConditionRanges() {
        return conditionRanges;
    }

    @Nullable
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "timeScale=" + timeScale.toString() +
                ", title='" + title + '\'' +
                ", dataType='" + dataType + '\'' +
                ", graphType='" + graphType.toString() + '\'' +
                ", minValue='" + minValue + '\'' +
                ", maxValue='" + maxValue + '\'' +
                ", sections='" + Arrays.toString(sections) + '\'' +
                ", conditionRanges='" + Arrays.toString(conditionRanges) + '\'' +
                ", annotations='" + Arrays.toString(annotations) + '\'' +
                '}';
    }


    public float getColumnWidthPercent() {
        if (timeScale == Trend.TimeScale.last_week) {
            return .1276f;
        } else if (timeScale == Trend.TimeScale.last_month) {
            return .0257f;
        } else {
            return .0111f;
        }
    }

    public float getColumnSpacePercent() {
        if (timeScale == Trend.TimeScale.last_week) {
            return .0176f;
        } else if (timeScale == Trend.TimeScale.last_month) {
            return .007f;
        } else {
            return 0;
        }
    }


    public enum GraphType implements Enums.FromString {
        NO_DATA,
        EMPTY,
        GRID,
        OVERVIEW,
        BAR,
        BUBBLES;

        public static GraphType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), EMPTY);
        }

        public static GraphType fromHash(int hashCode) {
            return Enums.fromHash(hashCode, values(), EMPTY);
        }
    }

    public enum DataType implements Enums.FromString {
        NONE,
        SCORES,
        HOURS,
        PERCENTS;

        public static DataType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }

}
