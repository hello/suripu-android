package is.hello.sense.api.model.v2.sensors;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SensorDataRequest implements Serializable {
    @SerializedName("sensors")
    private final List<SensorQuery> queries;

    public SensorDataRequest(@NonNull final List<Sensor> sensors) {
        this.queries = new ArrayList<>();
        for (final Sensor sensor : sensors) {
            queries.add(sensor.getSensorQuery());
        }
    }

    @Override
    public String toString() {
        return "SensorDataRequest{" +
                "Queries=" + Arrays.toString(queries.toArray()) +
                "}";
    }
}
