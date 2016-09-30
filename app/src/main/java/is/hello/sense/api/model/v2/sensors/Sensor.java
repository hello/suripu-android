package is.hello.sense.api.model.v2.sensors;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Scale;

/**
 * Represents an individual Sensor. This is returned from GET /v2/sensors.
 * Fetch the list of "sensorValues" over a given time period via POST /v2/sensors.
 */
public class Sensor extends ApiResponse {
    /**
     * Server value when data is missing.
     */
    public static final int NO_VALUE = -1;
    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private SensorType type;

    @SerializedName("unit")
    private SensorUnit unit;

    @SerializedName("message")
    private String message;

    @SerializedName("condition")
    private Condition condition;

    @SerializedName("value")
    private Double value;

    @SerializedName("scale")
    private List<Scale> scales;

    /**
     * Individual values over length of time, used for graphing. To be set after POST /v2/sensors
     */
    private float[] sensorValues = new float[0];

    /**
     * Updates {@link Sensor#sensorValues}. Use {@link SensorsDataResponse} returned from POST /v2/sensors.
     * Will use {@link Sensor#type} as the key in {@link SensorsDataResponse#getSensorData()}
     *
     * @param response The response from the POST.
     */
    public void setSensorValues(@NonNull final SensorsDataResponse response) {
        if (response.getSensorData().containsKey(getType())) {
            this.sensorValues = response.getSensorData().get(getType());
        }
    }

    public String getName() {
        return name;
    }

    public SensorType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Double getValue() {
        return value;
    }

    @ColorRes
    public int getColor() {
        return condition.colorRes;
    }

    public List<Scale> getScales() {
        return scales;
    }

    public float[] getSensorValues() {
        if (sensorValues == null) {
            return new float[0];
        }
        return sensorValues;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean hasBetterConditionThan(@NonNull final Sensor sensor){
        return condition.value > sensor.getCondition().value;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean hasBetterConditionThan(@NonNull final Sensor sensor){
        return condition.value > sensor.getCondition().value;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "Name=" + name +
                ", SensorType=" + type.toString() +
                ", Unit=" + unit.toString() +
                ", Message=" + message +
                ", Condition=" + condition +
                ", Scale=" + Arrays.toString(scales.toArray()) +
                ", Value=" + value +
                ", Values=" + Arrays.toString(sensorValues) +
                "}";

    }

}
