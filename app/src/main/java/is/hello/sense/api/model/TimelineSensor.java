package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimelineSensor extends ApiResponse {
    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    @JsonProperty("unit")
    private String unit;


    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }


    @Override
    public String toString() {
        return "TimelineSensor{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", unit='" + unit + '\'' +
                '}';
    }
}
