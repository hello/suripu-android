package is.hello.sense.api.model.v2.sensors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitPrinter;

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
    private List<Scale> scale;

    private float[] sensorValues = new float[0];
    private ValueLimits valueLimits = null;

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
    }

    @NonNull
    public float[] getSensorValues() {
        return sensorValues;
    }

    public int getColor(@NonNull final Context context) {
        return ContextCompat.getColor(context, condition.colorRes);
    }

    @NonNull
    public CharSequence getFormattedValue(@Nullable final UnitPrinter printer) {
        if (getValue() == null) {
            return "";
        } else if (printer != null) {
            return printer.print(getValue());
        } else {
            return Styles.assembleReadingAndUnit(getValue(), "");
        }
    }

    @NonNull
    public ValueLimits getValueLimits() {
        if (valueLimits == null) {
            valueLimits = new ValueLimits();
        }
        return valueLimits;
    }


    @Override
    public String toString() {
        return "Sensor{" +
                "Name=" + name +
                ", SensorType=" + type.toString() +
                ", Unit=" + unit.toString() +
                ", Message=" + message +
                ", Condition=" + condition +
                ", Value=" + value +
                ", Values=" + Arrays.toString(sensorValues) +
                ", ValueLimits=" + getValueLimits().toString() +
                "}";
    }

    public class ValueLimits {
        float min;
        float max;

        public ValueLimits() {
            for (final Float value : sensorValues) {
                if (value < min) {
                    min = value;
                    continue;
                }
                if (value > max) {
                    max = value;
                }
            }
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
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
