package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.functional.Lists;

public class RoomSensorHistory extends ApiResponse {
    @JsonProperty("humidity")
    private ArrayList<SensorHistory> humidity;

    @JsonProperty("light")
    private ArrayList<SensorHistory> light;

    @JsonProperty("particulates")
    private ArrayList<SensorHistory> particulates;

    @JsonProperty("sound")
    private ArrayList<SensorHistory> sound;

    @JsonProperty("temperature")
    private ArrayList<SensorHistory> temperature;


    public ArrayList<SensorHistory> getHumidity() {
        return humidity;
    }

    public ArrayList<SensorHistory> getLight() {
        return light;
    }

    public ArrayList<SensorHistory> getParticulates() {
        return particulates;
    }

    public ArrayList<SensorHistory> getSound() {
        return sound;
    }

    public ArrayList<SensorHistory> getTemperature() {
        return temperature;
    }

    public List<ArrayList<SensorHistory>> toList() {
        // Always change order of RoomConditions and RoomConditionsFragment too.
        return Lists.newArrayList(temperature, humidity, particulates, light, sound);
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
