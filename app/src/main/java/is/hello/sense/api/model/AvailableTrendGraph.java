package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AvailableTrendGraph extends ApiResponse {
    @JsonProperty("data_type")
    private TrendGraph.DataType dataType;

    @JsonProperty("time_period")
    private String timePeriod;


    public TrendGraph.DataType getDataType() {
        return dataType;
    }

    public String getTimePeriod() {
        return timePeriod;
    }


    @Override
    public String toString() {
        return "Trend{" +
                "dataType='" + dataType + '\'' +
                ", timePeriod='" + timePeriod + '\'' +
                '}';
    }
}
