package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorHistory extends ApiResponse {
    public static final String SENSOR_NAME_TEMPERATURE = "temperature";
    public static final String SENSOR_NAME_HUMIDITY = "humidity";
    public static final String SENSOR_NAME_PARTICULATES = "particulates";

    @JsonProperty("value")
    private float value;

    @JsonProperty("datetime")
    private long time;

    @JsonProperty("offset_millis")
    private long offset;


    public float getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }

    public long getOffset() {
        return offset;
    }


    @Override
    public String toString() {
        return "SensorHistory{" +
                "value=" + value +
                ", time=" + time +
                ", offset=" + offset +
                '}';
    }
}
