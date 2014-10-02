package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorState extends ApiResponse {
    @JsonProperty("value")
    private int value;

    @JsonProperty("message")
    private String message;

    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("last_updated_utc")
    private long lastUpdated;


    public int getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public Condition getCondition() {
        return condition;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }


    @Override
    public String toString() {
        return "SensorState{" +
                "value=" + value +
                ", message='" + message + '\'' +
                ", condition=" + condition +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
