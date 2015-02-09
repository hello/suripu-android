package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.functional.Lists;

public class RoomSensorHistory extends ApiResponse {
    @JsonProperty("humidity")
    private ArrayList<SensorGraphSample> humidity;

    @JsonProperty("light")
    private ArrayList<SensorGraphSample> light;

    @JsonProperty("particulates")
    private ArrayList<SensorGraphSample> particulates;

    @JsonProperty("sound")
    private ArrayList<SensorGraphSample> sound;

    @JsonProperty("temperature")
    private ArrayList<SensorGraphSample> temperature;


    public ArrayList<SensorGraphSample> getHumidity() {
        return humidity;
    }

    public ArrayList<SensorGraphSample> getLight() {
        return light;
    }

    @Deprecated
    public ArrayList<SensorGraphSample> getParticulates() {
        return particulates;
    }

    public ArrayList<SensorGraphSample> getSound() {
        return sound;
    }

    public ArrayList<SensorGraphSample> getTemperature() {
        return temperature;
    }

    public List<ArrayList<SensorGraphSample>> toList() {
        // Always change order of RoomConditions and RoomConditionsFragment too.
        return Lists.newArrayList(temperature, humidity, light, sound);
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
