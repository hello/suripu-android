package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TrendGraph extends ApiResponse {
    public static String TIME_PERIOD_DAY_OF_WEEK = "DOW";
    public static String TIME_PERIOD_OVER_TIME_1W = "1W";
    public static String TIME_PERIOD_OVER_TIME_2W = "2W";
    public static String TIME_PERIOD_OVER_TIME_1M = "1M";
    public static String TIME_PERIOD_OVER_TIME_3M = "3M";
    public static String TIME_PERIOD_OVER_TIME_ALL = "ALL";


    @JsonProperty("title")
    public String title;

    @JsonProperty("data_type")
    public DataType dataType;

    @JsonProperty("graph_type")
    public GraphType graphType;

    @JsonProperty("time_period")
    public String timePeriod;

    @JsonProperty("options")
    public List<String> options;

    @JsonProperty("data_points")
    public List<GraphSample> dataPoints;


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
        @JsonProperty("datetime")
        public long dateTime;

        @JsonProperty("y_value")
        public float yValue;

        @JsonProperty("x_value")
        public String xValue;

        @JsonProperty("offset_millis")
        public int offsetMillis;

        @JsonProperty("data_label")
        public DataLabel dataLabel;


        public long getDateTime() {
            return dateTime;
        }

        public float getyValue() {
            return yValue;
        }

        public String getxValue() {
            return xValue;
        }

        public int getOffsetMillis() {
            return offsetMillis;
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
                    ", offsetMillis=" + offsetMillis +
                    ", dataLabel=" + dataLabel +
                    '}';
        }
    }


    public enum DataType {
        NONE,
        SLEEP_SCORE,
        SLEEP_DURATION;

        public String toQueryString() {
            return toString().toLowerCase();
        }

        @JsonCreator
        public static DataType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }

    public static enum DataLabel {
        BAD,
        OK,
        GOOD;

        @JsonCreator
        public static DataLabel fromString(@Nullable String string) {
            return Enums.fromString(string, values(), OK);
        }
    }

    public static enum GraphType {
        HISTOGRAM,
        TIME_SERIES_LINE;

        @JsonCreator
        public static GraphType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), TIME_SERIES_LINE);
        }
    }
}
