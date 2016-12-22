package is.hello.sense.api.model.v2.sensors;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class SensorResponse extends ApiResponse {
    @SerializedName("status")
    private SensorStatus status;

    @SerializedName("sensors")
    private List<Sensor> sensors;

    public SensorResponse(@NonNull final List<Sensor> sensors,
                          @NonNull final SensorStatus sensorStatus){
        this.sensors = sensors;
        this.status = sensorStatus;
    }

    @Override
    public String toString() {
        return "SensorResponse{" +
                "Status=" + status.toString() +
                ", Sensors=" + Arrays.toString(sensors.toArray()) +
                "}";
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public SensorStatus getStatus() {
        return status;
    }
}
