package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SensorQuery implements Serializable {

    @SerializedName("type")
    private final SensorType type;

    @SerializedName("unit")
    private final SensorUnit unit;

    @SerializedName("scope")
    private final QueryScope scope;

    @SerializedName("aggregation_method")
    private final AggregationMethod aggregationMethod;

    public SensorQuery(@NonNull final SensorType type,
                       @NonNull final SensorUnit unit,
                       @NonNull final QueryScope scope,
                       @NonNull final AggregationMethod aggregationMethod) {
        this.type = type;
        this.unit = unit;
        this.scope = scope;
        this.aggregationMethod = aggregationMethod;
    }


    @Override
    public String toString() {
        return "SensorQuery{" +
                "Type=" + type.toString() +
                ", Unit=" + unit +
                ", Scope=" + scope +
                ", AggregationMethod=" + aggregationMethod +
                "}";
    }
}
