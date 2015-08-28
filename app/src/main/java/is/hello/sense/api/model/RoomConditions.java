package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.ApiService;

public class RoomConditions extends ApiResponse {
    @SerializedName("temperature")
    @VisibleForTesting SensorState temperature;

    @SerializedName("humidity")
    @VisibleForTesting SensorState humidity;

    @SerializedName("particulates")
    @VisibleForTesting SensorState particulates;

    @SerializedName("light")
    @VisibleForTesting SensorState light;

    @SerializedName("sound")
    @VisibleForTesting SensorState sound;


    public SensorState getTemperature() {
        if (temperature != null) {
            temperature.setName(ApiService.SENSOR_NAME_TEMPERATURE);
        }
        return temperature;
    }

    public SensorState getHumidity() {
        if (humidity != null) {
            humidity.setName(ApiService.SENSOR_NAME_HUMIDITY);
        }
        return humidity;
    }

    public SensorState getParticulates() {
        if (particulates != null) {
            particulates.setName(ApiService.SENSOR_NAME_PARTICULATES);
        }
        return particulates;
    }

    public SensorState getLight() {
        if (light != null) {
            light.setName(ApiService.SENSOR_NAME_LIGHT);
        }
        return light;
    }

    public SensorState getSound() {
        if (sound != null) {
            sound.setName(ApiService.SENSOR_NAME_SOUND);
        }
        return sound;
    }

    public boolean isEmpty() {
        return ((temperature == null || temperature.getValue() == null) &&
                (humidity == null || humidity.getValue() == null) &&
                (particulates == null || particulates.getValue() == null) &&
                (light == null || light.getValue() == null) &&
                (sound == null || sound.getValue() == null));
    }

    public List<SensorState> toList() {
        final List<SensorState> sensors = new ArrayList<>(5);

        final SensorState temperature = getTemperature();
        if (temperature != null) {
            sensors.add(temperature);
        }

        final SensorState humidity = getHumidity();
        if (humidity != null) {
            sensors.add(humidity);
        }

        final SensorState particulates = getParticulates();
        if (particulates != null) {
            sensors.add(particulates);
        }

        final SensorState light = getLight();
        if (light != null) {
            sensors.add(light);
        }

        final SensorState sound = getSound();
        if (sound != null) {
            sensors.add(sound);
        }

        return sensors;
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
