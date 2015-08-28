package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.gson.Enums;

public class TrendGraph extends ApiResponse {
    public static final String TIME_PERIOD_DAY_OF_WEEK = "DOW";
    public static final String TIME_PERIOD_OVER_TIME_1W = "1W";
    public static final String TIME_PERIOD_OVER_TIME_2W = "2W";
    public static final String TIME_PERIOD_OVER_TIME_1M = "1M";
    public static final String TIME_PERIOD_OVER_TIME_3M = "3M";
    public static final String TIME_PERIOD_OVER_TIME_ALL = "ALL";


    @SerializedName("title")
    private String title;

    @SerializedName("data_type")
    private DataType dataType;

    @SerializedName("graph_type")
    private GraphType graphType;

    @SerializedName("time_period")
    private String timePeriod;

    @SerializedName("options")
    private List<String> options;

    @SerializedName("data_points")
    private List<GraphSample> dataPoints;


    public String getTitle() {
        return title;
    }

    public DataType getDataType() {
        return dataType;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public String getTimePeriod() {
        return timePeriod;
    }

    public List<String> getOptions() {
        return options;
    }

    public List<GraphSample> getDataPoints() {
        return dataPoints;
    }

    public String formatSampleDate(@NonNull GraphSample sample) {
        DateTime dateTime = sample.getShiftedDateTime();
        switch (getTimePeriod()) {
            case TIME_PERIOD_DAY_OF_WEEK: {
                return dateTime.toString("hh:mm");
            }

            case TIME_PERIOD_OVER_TIME_1W: {
                return dateTime.toString("E").substring(0, 1);
            }

            case TIME_PERIOD_OVER_TIME_2W: {
                if (dataPoints.size() < 5) {
                    return dateTime.toString("MMM d");
                }
                // intentional fall-through
            }

            case TIME_PERIOD_OVER_TIME_1M: {
                return dateTime.toString("M/d");
            }

            default:
            case TIME_PERIOD_OVER_TIME_ALL:
            case TIME_PERIOD_OVER_TIME_3M: {
                return dateTime.toString("MMM");
            }
        }
    }


    @Override
    public String toString() {
        return "TrendGraph{" +
                "title='" + title + '\'' +
                ", dataType='" + dataType + '\'' +
                ", graphType=" + graphType +
                ", timePeriod='" + timePeriod + '\'' +
                ", options=" + options +
                ", dataPoints=" + dataPoints +
                '}';
    }


    public static class GraphSample extends ApiResponse {
        @SerializedName("datetime")
        private DateTime dateTime;

        @SerializedName("y_value")
        private float yValue;

        @SerializedName("x_value")
        private String xValue;

        @SerializedName("offset_millis")
        private int offset;

        @SerializedName("data_label")
        private DataLabel dataLabel;


        public float getYValue() {
            return yValue;
        }

        public String getXValue() {
            return xValue;
        }

        public DateTimeZone getTimeZone() {
            return DateTimeZone.forOffsetMillis(offset);
        }

        public DateTime getShiftedDateTime() {
            return dateTime.withZone(getTimeZone());
        }

        public DataLabel getDataLabel() {
            return dataLabel;
        }


        @Override
        public String toString() {
            return "GraphSample{" +
                    "dateTime=" + dateTime +
                    ", yValue=" + yValue +
                    ", xValue='" + xValue + '\'' +
                    ", offset=" + offset +
                    ", dataLabel=" + dataLabel +
                    '}';
        }


        @VisibleForTesting
        public static class Builder {
            private final GraphSample graphSample = new GraphSample();

            public Builder() {
                DateTimeZone timeZone = DateTimeZone.getDefault();
                setDateTime(DateTime.now(timeZone));
                setOffset(timeZone.getOffset(graphSample.dateTime));
                setXValue("");
            }

            public Builder setDateTime(DateTime dateTime) {
                graphSample.dateTime = dateTime;
                return this;
            }

            public Builder setYValue(float yValue) {
                graphSample.yValue = yValue;
                return this;
            }

            public Builder setXValue(String xValue) {
                graphSample.xValue = xValue;
                return this;
            }

            public Builder setOffset(int offset) {
                graphSample.offset = offset;
                return this;
            }

            public Builder setDataLabel(DataLabel dataLabel) {
                graphSample.dataLabel = dataLabel;
                return this;
            }

            public GraphSample build() {
                return graphSample;
            }
        }
    }


    public enum DataType implements Enums.FromString {
        NONE,
        SLEEP_SCORE,
        SLEEP_DURATION;

        public String toQueryString() {
            return toString().toLowerCase();
        }

        public static DataType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }

    public enum DataLabel implements Enums.FromString {
        BAD,
        OK,
        GOOD;

        public static DataLabel fromString(@Nullable String string) {
            return Enums.fromString(string, values(), OK);
        }
    }


    @VisibleForTesting
    public static class Builder {
        private final TrendGraph trendGraph = new TrendGraph();

        public Builder() {
            trendGraph.options = new ArrayList<>();
            trendGraph.dataPoints = new ArrayList<>();
        }

        public Builder setTitle(String title) {
            trendGraph.title = title;
            return this;
        }

        public Builder setDataType(DataType dataType) {
            trendGraph.dataType = dataType;
            return this;
        }

        public Builder setTimePeriod(String timePeriod) {
            trendGraph.timePeriod = timePeriod;
            return this;
        }

        public Builder addOption(@NonNull String option) {
            trendGraph.options.add(option);
            return this;
        }

        public Builder addDataPoint(@NonNull GraphSample dataPoint) {
            trendGraph.dataPoints.add(dataPoint);
            return this;
        }

        public Builder duplicateDataPoint(int position, int times) {
            GraphSample graphSample = trendGraph.dataPoints.get(position);
            for (int i = 0; i < times; i++) {
                trendGraph.dataPoints.add(graphSample);
            }
            return this;
        }

        public TrendGraph build() {
            return trendGraph;
        }
    }
}
