package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.functional.Lists;

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
        // This order applies to:
        // - RoomSensorHistory
        // - RoomConditions
        // - RoomConditionsFragment
        // - UnitSystem
        // - OnboardingRoomCheckFragment
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
