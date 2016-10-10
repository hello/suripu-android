package is.hello.sense.api.model.v2.sensors;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class SensorsDataResponse extends ApiResponse {


    @SerializedName("sensors")
    private SensorData sensorData;


    @SerializedName("timestamps")
    private List<X> timestamps;

    public SensorsDataResponse() {
        this.sensorData = new SensorData();
        this.timestamps = new ArrayList<>(0);
    }

    public SensorsDataResponse(final SensorData sensorData, final List<X> timestamps) {
        this.sensorData = sensorData;
        this.timestamps = timestamps;
    }

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
