package is.hello.sense.api.model.v2.sensors;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SensorsDataResponse implements Serializable {


    @SerializedName("sensors")
    private SensorData sensorData;


    @SerializedName("timestamps")
    private List<X> timestamps;

    @Override
    public String toString() {
        return "SensorsDataResponse{" +
                "SensorData=" + sensorData.toString() +
                ", Timestamps=" + Arrays.toString(timestamps.toArray()) +
                "}";
    }

    public SensorData getSensorData() {
        return sensorData;
    }

    public List<X> getTimestamps() {
        return timestamps;
    }
}
