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
        temperature.setName(ApiService.SENSOR_NAME_TEMPERATURE);
        return temperature;
    }

    public SensorState getHumidity() {
        temperature.setName(ApiService.SENSOR_NAME_HUMIDITY);
        return humidity;
    }

    public SensorState getParticulates() {
        temperature.setName(ApiService.SENSOR_NAME_PARTICULATES);
        return particulates;
    }

    public SensorState getLight() {
        temperature.setName(ApiService.SENSOR_NAME_LIGHT);
        return light;
    }

    public SensorState getSound() {
        temperature.setName(ApiService.SENSOR_NAME_SOUND);
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
        return Lists.newArrayList(getTemperature(), getHumidity(), getLight(), getSound());
    }

    public @Nullable SensorState getSensorStateWithName(@NonNull String name) {
        switch (name) {
            case ApiService.SENSOR_NAME_HUMIDITY:
                return getHumidity();

            case ApiService.SENSOR_NAME_PARTICULATES:
                return getParticulates();

            case ApiService.SENSOR_NAME_TEMPERATURE:
                return getTemperature();

            case ApiService.SENSOR_NAME_LIGHT:
                return getLight();

            case ApiService.SENSOR_NAME_SOUND:
                return getSound();

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
