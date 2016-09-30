package is.hello.sense.api.model.v2.sensors;

import android.content.Context;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitOperations;

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
     * Min / Max Values
     */
    private ValueLimits valueLimits = null;

    /**
     * Symbol for Sensor. To be set after POST /v2/sensors
     */
    private String sensorSuffix = "";

    /**
     * True by default. If false will use Fahrenheit.
     */
    private boolean useCelsius = true;


    private List<X> sensorTimestamps = null;
    private QueryScope queryScopeForTimestamps = null;

    /**
     * Updates {@link Sensor#sensorValues}. Use {@link SensorsDataResponse} returned from POST /v2/sensors.
     * Will use {@link Sensor#type} as the key in {@link SensorsDataResponse#getSensorData()}
     *
     * @param response The response from the POST.
     */
    public void setSensorValues(@NonNull final SensorsDataResponse response) {
        if (response.getSensorData().containsKey(getType())) {
            this.sensorValues = response.getSensorData().get(getType());
            updateValueLimits();
        }
    }

    public void setSensorTimestamps(@NonNull final List<X> sensorTimestamps, @NonNull final QueryScope queryScope) {
        this.sensorTimestamps = sensorTimestamps;
        this.queryScopeForTimestamps = queryScope;
    }

    /**
     * @param sensorSuffix suffix to be shown.
     */
    public void setSensorSuffix(@NonNull final String sensorSuffix) {
        this.sensorSuffix = sensorSuffix;
    }

    /**
     * @param useCelsius false to show Fahrenheit
     */
    public void setUseCelsius(final boolean useCelsius) {
        this.useCelsius = useCelsius;
        updateValueLimits();
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
        if (!useCelsius && getType() == SensorType.TEMPERATURE) {
            return UnitOperations.celsiusToFahrenheit(value);
        }
        return value;
    }

    public int getColor(@NonNull final Context context) {
        return ContextCompat.getColor(context, condition.colorRes);
    }

    public List<Scale> getScales() {
        return scales;
    }

    public float[] getSensorValues() {
        return sensorValues;
    }

    /**
     * Return this Sensor's {@link Sensor#value} formatted.
     *
     * @param withSuffix when true will append the sensors {@link Sensor#sensorSuffix} as a superscript.
     * @return Formatted
     */
    @NonNull
    public CharSequence getFormattedValue(final boolean withSuffix) {
        if (getValue() == null) {
            return "";
        }
        return Styles.assembleReadingAndUnit(getValue(), withSuffix ? sensorSuffix : "");
    }

    /**
     * Contains a {@link is.hello.sense.api.model.v2.sensors.Sensor.ValueLimits#min} and
     * {@link is.hello.sense.api.model.v2.sensors.Sensor.ValueLimits#max} value corresponding to
     * {@link Sensor#sensorValues}
     *
     * @return {@link Sensor#valueLimits}
     */
    @NonNull
    public CharSequence getFormattedValueAtPosition(final int position) {
        if (getSensorValues().length <= position) {
            return "";
        }
        float value = getSensorValues()[position];
        if (!useCelsius && getType() == SensorType.TEMPERATURE) {
            value = UnitOperations.celsiusToFahrenheit(value);
        }
        return Styles.assembleReadingAndUnit(value, sensorSuffix);

    }

    /**
     * Contains a {@link is.hello.sense.api.model.v2.sensors.Sensor.ValueLimits#min} and
     * {@link is.hello.sense.api.model.v2.sensors.Sensor.ValueLimits#max} value corresponding to
     * {@link Sensor#sensorValues}
     *
     * @return {@link Sensor#valueLimits}
     */
    @NonNull
    public ValueLimits getValueLimits() {
        if (valueLimits == null) {
            updateValueLimits();
        }
        return valueLimits;
    }

    /**
     * @return suffix for this string. Empty string if none exists.
     */
    @NonNull
    public String getSensorSuffix() {
        return sensorSuffix;
    }

    /**
     * @return {@link ApiService#UNIT_TEMPERATURE_CELSIUS} or
     * {@link ApiService#UNIT_TEMPERATURE_US_CUSTOMARY}
     */
    @NonNull
    public String getTemperatureSymbol() {
        if (useCelsius) {
            return ApiService.UNIT_TEMPERATURE_CELSIUS;
        }
        return ApiService.UNIT_TEMPERATURE_US_CUSTOMARY;
    }


    public long getTimestampForPosition(final int position) {
        if (sensorTimestamps == null || sensorTimestamps.size() <= position) {
            return NO_VALUE;
        }
        return sensorTimestamps.get(position).getTimestamp();
    }

    public QueryScope getQueryScopeForTimestamps() {
        return queryScopeForTimestamps;
    }

    private void updateValueLimits() {
        valueLimits = new ValueLimits();
    }


    @StringRes
    public int getAboutStringRes() {
        switch (getType()) {
            case TEMPERATURE:
                if (useCelsius) {
                    return R.string.sensor_about_temperature_celsius;
                } else {
                    return R.string.sensor_about_temperature_fahrenheit;
                }
            case HUMIDITY:
                return R.string.sensor_about_humidity;
            case LIGHT:
                return R.string.sensor_about_light;
            case CO2:
                return R.string.sensor_about_co2;
            case LIGHT_TEMPERATURE:
                return R.string.sensor_about_light_temp;
            case PARTICULATES:
                return R.string.sensor_about_particulates;
            case SOUND:
                return R.string.sensor_about_noise;
            case UV:
                return R.string.sensor_about_uv_light;
            case TVOC:
                return R.string.sensor_about_voc;
            default:
                throw new IllegalArgumentException("No string found for type: " + getType());
        }
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
                ", ValueLimits=" + getValueLimits().toString() +
                "}";
    }


    /**
     * Helper class for {@link Sensor} to track the min and max {@link Sensor#sensorValues}.
     */
    public class ValueLimits implements Serializable {
        Float min = null;
        Float max = null;
        final String formattedMin;
        final String formattedMax;

        public ValueLimits() {
            if (sensorValues == null || sensorValues.length == 0) {
                min = (float)NO_VALUE;
                max = (float)NO_VALUE;
            } else {
                for (final Float value : sensorValues) {
                    if (value <= NO_VALUE) {
                        continue;
                    }
                    if (min == null || value < min) {
                        min = value;
                    }
                    if (max == null || value > max) {
                        max = value;
                    }
                }
            }
            if (max != null && min != null && Math.floor(min) == Math.ceil(max)) {
                formattedMax = "";
                formattedMin = formatValue(min);
            } else {
                formattedMin = formatValue(min);
                formattedMax = formatValue(max);
            }
        }


        @NonNull
        private String formatValue(@Nullable Float value) {
            if (value == null || value == NO_VALUE) {
                return "";
            }
            if (!useCelsius && getType() == SensorType.TEMPERATURE) {
                value = UnitOperations.celsiusToFahrenheit(value);
            }
            if (getType() == SensorType.LIGHT) {
                return String.format("%.1f", value);

            } else {
                return String.format("%.0f", value);
            }
        }

        @NonNull
        public Float getMin() {
            return min;
        }

        @NonNull
        public Float getMax() {
            return max;
        }

        @NonNull
        public String getFormattedMin() {
            return formattedMin;
        }

        @NonNull
        public String getFormattedMax() {
            return formattedMax;
        }

        @Override
        public String toString() {
            return "ValueLimits{" +
                    "min=" + min +
                    ", max=" + max +
                    "}";
        }
    }

}
