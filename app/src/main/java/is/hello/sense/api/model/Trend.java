package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Trend extends ApiResponse {
    @JsonProperty("data_type")
    private String dataType;

    @JsonProperty("time_period")
    private String timePeriod;


    public String getDataType() {
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
