package is.hello.sense.api.model.v2.sensors;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class SensorDataRequest extends ApiResponse {

    @SerializedName("scope")
    private final QueryScope queryScope;

    @SerializedName("sensors")
    private final List<SensorType> queries;

    public SensorDataRequest(@NonNull final QueryScope queryScope, @NonNull final List<Sensor> sensors) {
        this.queryScope = queryScope;
        this.queries = new ArrayList<>();
        for (final Sensor sensor : sensors) {
            queries.add(sensor.getType());
        }
    }

    @Override
    public String toString() {
        return "SensorDataRequest{" +
                "Scope=" + queryScope +
                "Queries=" + Arrays.toString(queries.toArray()) +
                "}";
    }
}
