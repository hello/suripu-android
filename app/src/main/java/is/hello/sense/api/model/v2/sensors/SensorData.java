package is.hello.sense.api.model.v2.sensors;


import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class SensorData extends HashMap<SensorType, float[]> implements Serializable {

    @Override
    public String toString() {
        return "SensorData{" +
                "Key=" + Arrays.toString(keySet().toArray()) +
                "Sensors=" + Arrays.toString(values().toArray()) +
                "}";
    }
}
