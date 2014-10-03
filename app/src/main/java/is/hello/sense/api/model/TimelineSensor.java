package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimelineSensor extends ApiResponse {
    public static final String NAME_TEMPERATURE = "temperature";
    public static final String NAME_HUMIDITY = "humidity";
    public static final String NAME_PARTICULATES = "particulates";

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private long value;

    @JsonProperty("unit")
    private String unit;


    void setName(String name) {
        this.name = name;
    }

    void setValue(long value) {
        this.value = value;
    }

    void setUnit(String unit) {
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
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
