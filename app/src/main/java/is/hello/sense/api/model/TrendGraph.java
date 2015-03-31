package is.hello.sense.api.model;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

import is.hello.sense.R;

public class TrendGraph extends ApiResponse {
    public static final String TIME_PERIOD_DAY_OF_WEEK = "DOW";
    public static final String TIME_PERIOD_OVER_TIME_1W = "1W";
    public static final String TIME_PERIOD_OVER_TIME_2W = "2W";
    public static final String TIME_PERIOD_OVER_TIME_1M = "1M";
    public static final String TIME_PERIOD_OVER_TIME_3M = "3M";
    public static final String TIME_PERIOD_OVER_TIME_ALL = "ALL";


    @JsonProperty("title")
    private String title;

    @JsonProperty("data_type")
    private DataType dataType;

    @JsonProperty("graph_type")
    private GraphType graphType;

    @JsonProperty("time_period")
    private String timePeriod;

    @JsonProperty("options")
    private List<String> options;

    @JsonProperty("data_points")
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
                return dateTime.toString("E");
            }

            case TIME_PERIOD_OVER_TIME_2W: {
                return dateTime.toString("MMM d");
            }

            case TIME_PERIOD_OVER_TIME_1M:
            case TIME_PERIOD_OVER_TIME_3M: {
                return dateTime.toString("M/d");
            }

            default:
            case TIME_PERIOD_OVER_TIME_ALL: {
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
        @JsonProperty("datetime")
        private DateTime dateTime;

        @JsonProperty("y_value")
        private float yValue;

        @JsonProperty("x_value")
        private String xValue;

        @JsonProperty("offset_millis")
        private int offset;

        @JsonProperty("data_label")
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
        BAD(R.color.sensor_alert),
        OK(R.color.sensor_warning),
        GOOD(R.color.sensor_ideal);

        public final @ColorRes int colorRes;

        DataLabel(@ColorRes int colorRes) {
            this.colorRes = colorRes;
        }

        @JsonCreator
        public static DataLabel fromString(@Nullable String string) {
            return Enums.fromString(string, values(), OK);
        }
    }

}
