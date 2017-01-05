package is.hello.sense.api.model.v2.sensors;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.Constants;

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

    /**
     * Remove last -1 invalid value if at end of list.
     * Can remove method once server takes care of truncating for us.
     */
    public void removeLastInvalidSensorDataValues() {
        for (final Map.Entry<SensorType, float[]> entry : sensorData.entrySet()) {
            final float[] origValues = entry.getValue();
            final int length = origValues.length;
            if (length >= 1 && origValues[length - 1] == Constants.NONE) {
                entry.setValue(Arrays.copyOf(origValues, length - 1));
            }
        }
    }
}
