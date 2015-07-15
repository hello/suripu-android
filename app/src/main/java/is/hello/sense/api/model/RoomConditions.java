package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.functional.Lists;

public class RoomConditions extends ApiResponse {
    @SerializedName("temperature")
    private SensorState temperature;

    @SerializedName("humidity")
    private SensorState humidity;

    @SerializedName("particulates")
    private SensorState particulates;

    @SerializedName("light")
    private SensorState light;

    @SerializedName("sound")
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

    public boolean isEmpty() {
        return ((temperature == null || temperature.getValue() == null) &&
                (humidity == null || humidity.getValue() == null) &&
                (light == null || light.getValue() == null) &&
                (sound == null || sound.getValue() == null));
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
