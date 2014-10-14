package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

public class SensorState extends ApiResponse {
    @JsonProperty("value")
    private Long value;

    @JsonProperty("message")
    private String message;

    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("last_updated_utc")
    private DateTime lastUpdated;


    public Long getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public Condition getCondition() {
        return condition;
    }

    public DateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "SensorState{" +
                "value=" + value +
                ", message='" + message + '\'' +
                ", condition=" + condition +
                ", unit=" + unit +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
