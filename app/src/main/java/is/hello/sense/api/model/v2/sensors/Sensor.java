package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

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
    private Float value; // Will be null when status is "waiting for data"

    @SerializedName("scale")
    private List<Scale> scales;

    /**
     * Individual values over length of time, used for graphing. To be set after POST /v2/sensors
     */
    private float[] sensorValues = new float[0];

    public Sensor(@NonNull final String name,
                  @NonNull final SensorType type,
                  @NonNull final SensorUnit unit,
                  @NonNull final String message,
                  @NonNull final Condition condition,
                  @Nullable final Float value,
                  @NonNull final List<Scale> scales){
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.message = message;
        this.condition = condition;
        this.value = value;
        this.scales = scales;
    }

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

    public float getValue() {
        if (value == null) {
            return NO_VALUE;
        }
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

    public boolean hasBetterConditionThan(@NonNull final Sensor sensor) {
        // ide complains about simplifying. Basically if a sensor's condition is not available, treat the other sensor as better condition.
        return condition == null || sensor.condition != null && condition.value > sensor.getCondition().value;
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

    @VisibleForTesting
    public static Sensor newTemperatureTestCase(final Float value) {
        return new Sensor("temperature",
                          SensorType.TEMPERATURE,
                          SensorUnit.CELCIUS,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newHumidityTestCase(final Float value) {
        return new Sensor("humidity",
                          SensorType.HUMIDITY,
                          SensorUnit.PERCENT,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newLightTestCase(final Float value) {
        return new Sensor("light",
                          SensorType.LIGHT,
                          SensorUnit.LUX,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newParticulatesTestCase(final Float value) {
        return new Sensor("particulates",
                          SensorType.PARTICULATES,
                          SensorUnit.PPM,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newSoundTestCase(final Float value) {
        return new Sensor("sound",
                          SensorType.SOUND,
                          SensorUnit.DB,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newCO2TestCase(final Float value) {
        return new Sensor("co2",
                          SensorType.CO2,
                          SensorUnit.PPM,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newVOCTestCase(final Float value) {
        return new Sensor("voc",
                          SensorType.TVOC,
                          SensorUnit.VOC,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newLightTemperatureTestCase(final Float value) {
        return new Sensor("light temperature",
                          SensorType.LIGHT_TEMPERATURE,
                          SensorUnit.KELVIN,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newUVTestCase(final Float value) {
        return new Sensor("uv",
                          SensorType.UV,
                          SensorUnit.UNKNOWN,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newPressureTestCase(final Float value) {
        return new Sensor("pressure",
                          SensorType.PRESSURE,
                          SensorUnit.MILLIBAR,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static Sensor newUnknownTestCase(final Float value) {
        return new Sensor("unknown",
                          SensorType.UNKNOWN,
                          SensorUnit.UNKNOWN,
                          "message",
                          Condition.IDEAL,
                          value,
                          Scale.generateTestScale());
    }

    @VisibleForTesting
    public static List<Sensor> generateTestCaseList() {
        return Arrays.asList(Sensor.newTemperatureTestCase(0f),
                             Sensor.newHumidityTestCase(1f),
                             Sensor.newLightTestCase(2f),
                             Sensor.newParticulatesTestCase(3f),
                             Sensor.newSoundTestCase(4f),
                             Sensor.newCO2TestCase(5f),
                             Sensor.newVOCTestCase(6f),
                             Sensor.newLightTemperatureTestCase(7f),
                             Sensor.newUVTestCase(8f),
                             Sensor.newPressureTestCase(9f),
                             Sensor.newUnknownTestCase(10f));
    }
}
