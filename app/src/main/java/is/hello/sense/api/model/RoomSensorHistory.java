package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;

public class RoomSensorHistory extends ApiResponse {
    @SerializedName("humidity")
    private ArrayList<SensorGraphSample> humidity;

    @SerializedName("light")
    private ArrayList<SensorGraphSample> light;

    @SerializedName("particulates")
    private ArrayList<SensorGraphSample> particulates;

    @SerializedName("sound")
    private ArrayList<SensorGraphSample> sound;

    @SerializedName("temperature")
    private ArrayList<SensorGraphSample> temperature;


    public ArrayList<SensorGraphSample> getHumidity() {
        return humidity;
    }

    public ArrayList<SensorGraphSample> getLight() {
        return light;
    }

    public ArrayList<SensorGraphSample> getParticulates() {
        return particulates;
    }

    public ArrayList<SensorGraphSample> getSound() {
        return sound;
    }

    public ArrayList<SensorGraphSample> getTemperature() {
        return temperature;
    }

    public ArrayList<SensorGraphSample> getSamplesForSensor(@NonNull String name) {
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
        return "RoomSensorHistory{" +
                "humidity=" + humidity +
                ", light=" + light +
                ", particulates=" + particulates +
                ", sound=" + sound +
                ", temperature=" + temperature +
                '}';
    }
}
