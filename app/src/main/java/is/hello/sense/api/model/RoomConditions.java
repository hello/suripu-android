package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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


    public @Nullable SensorState getSensorStateWithName(@NonNull String name) {
        switch (name) {
            case SensorHistory.SENSOR_NAME_HUMIDITY:
                return humidity;

            case SensorHistory.SENSOR_NAME_PARTICULATES:
                return particulates;

            case SensorHistory.SENSOR_NAME_TEMPERATURE:
                return temperature;

            default:
                return null;
        }
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
