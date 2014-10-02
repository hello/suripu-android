package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomConditions extends ApiResponse {
    @JsonProperty("temperature")
    private SensorState temperature;

    @JsonProperty("humidity")
    private SensorState humidity;

    @JsonProperty("particulates")
    private SensorState particulates;


    public SensorState getTemperature() {
        return temperature;
    }

    public SensorState getHumidity() {
        return humidity;
    }

    public SensorState getParticulates() {
        return particulates;
    }


    @Override
    public String toString() {
        return "RoomConditions{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", particulates=" + particulates +
                '}';
    }
}
