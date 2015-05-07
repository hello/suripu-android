package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.functional.Lists;

public class RoomConditions extends ApiResponse {
    @JsonProperty("temperature")
    private SensorState temperature;

    @JsonProperty("humidity")
    private SensorState humidity;

    @JsonProperty("particulates")
    private SensorState particulates;

    @JsonProperty("light")
    private SensorState light;

    @JsonProperty("sound")
    private SensorState sound;


    public SensorState getTemperature() {
        return temperature;
    }

    public SensorState getHumidity() {
        return humidity;
    }

    public SensorState getParticulates() {
        return particulates;
    }

    public SensorState getLight() {
        return light;
    }

    public SensorState getSound() {
        return sound;
    }

    public List<SensorState> toList() {
        // This order applies to:
        // - RoomSensorHistory
        // - RoomConditions
        // - RoomConditionsFragment
        // - UnitSystem
        // - OnboardingRoomCheckFragment
        return Lists.newArrayList(temperature, humidity, light, sound);
    }

    public @Nullable SensorState getSensorStateWithName(@NonNull String name) {
        switch (name) {
            case ApiService.SENSOR_NAME_HUMIDITY:
                return humidity;

            case ApiService.SENSOR_NAME_PARTICULATES:
                return particulates;

            case ApiService.SENSOR_NAME_TEMPERATURE:
                return temperature;

            case ApiService.SENSOR_NAME_LIGHT:
                return light;

            case ApiService.SENSOR_NAME_SOUND:
                return sound;

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
                ", light=" + light +
                ", sound=" + sound +
                '}';
    }
}
