package is.hello.sense.api.model.v2.sensors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitPrinter;

/**
 * Represents an individual Sensor. This is returned from GET /v2/sensors.
 * Fetch the list of "sensorValues" over a given time period via POST /v2/sensors.
 */
public class Sensor implements Serializable {
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

    @NonNull
    public SensorQuery getSensorQuery() {
        return new SensorQuery(type,
                               unit,
                               QueryScope.LAST_3H_5_MINUTE,
                               AggregationMethod.AVG);
    }

    public void setSensorValues(@NonNull final float[] values) {
        this.sensorValues = values;
        getValueLimits();

    }

    public void setSensorSuffix(@NonNull final String sensorSuffix) {
        this.sensorSuffix = sensorSuffix;
    }

    @NonNull
    public float[] getSensorValues() {
        return sensorValues;
    }

    public int getColor(@NonNull final Context context) {
        return ContextCompat.getColor(context, condition.colorRes);
    }

    @NonNull
    public CharSequence getFormattedValue(final boolean withSuffix) {
        return Styles.assembleReadingAndUnit(getValue(), withSuffix ? sensorSuffix : "");
    }

    @NonNull
    public ValueLimits getValueLimits() {
        if (valueLimits == null) {
            valueLimits = new ValueLimits();
        }
        return valueLimits;
    }

    @NonNull
    public String getSensorSuffix() {
        return sensorSuffix;
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
     * Helper class for {@link Sensor} to track the min and max sensorValues.
     */
    public class ValueLimits implements Serializable {
        Float min = null;
        Float max = null;
        final String formattedMin;
        final String formattedMax;

        public ValueLimits() {
            if (sensorValues == null || sensorValues.length == 0) {
                min = -1f;
                max = -1f;
            } else {
                for (final Float value : sensorValues) {
                    if (min == null || value < min) {
                        min = value;
                    }
                    if (max == null || value > max) {
                        max = value;
                    }
                }
            }
            if (max != null && min != null && Math.round(min) == Math.round(max)) {
                formattedMax = "";
                formattedMin = formatValue(min);
            } else {
                formattedMin = formatValue(min);
                formattedMax = formatValue(max);
            }
        }


        @NonNull
        private String formatValue(@Nullable final Float value) {
            if (value == null || value == -1) {
                return "";
            }
            return String.format("%.0f", value);
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
