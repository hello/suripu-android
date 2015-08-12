package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitPrinter;

public class SensorState extends ApiResponse {
    @SerializedName("value")
    private Double value;

    @SerializedName("message")
    private String message;

    @SerializedName("condition")
    private Condition condition;

    @SerializedName("unit")
    private String unit;

    @SerializedName("last_updated_utc")
    private DateTime lastUpdated;

    @SerializedName("ideal_conditions")
    private String idealConditions;


    public SensorState() {
    }

    public SensorState(double value,
                       @NonNull String message,
                       @NonNull Condition condition,
                       @NonNull String unit,
                       @NonNull DateTime lastUpdated) {
        this.value = value;
        this.message = message;
        this.condition = condition;
        this.unit = unit;
        this.lastUpdated = lastUpdated;
    }

    public Double getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public Condition getCondition() {
        return condition;
    }

    public DateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getUnit() {
        return unit;
    }

    public String getIdealConditions() {
        return idealConditions;
    }

    public @Nullable CharSequence getFormattedValue(@Nullable UnitPrinter formatter) {
        if (getValue() == null) {
            return null;
        } else if (formatter != null) {
            return formatter.print(getValue().longValue());
        } else {
            return Styles.assembleReadingAndUnit(getValue().longValue(), getUnit());
        }
    }

    @Override
    public String toString() {
        return "SensorState{" +
                "value=" + value +
                ", message='" + message + '\'' +
                ", condition=" + condition +
                ", unit='" + unit + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", idealConditions='" + idealConditions + '\'' +
                '}';
    }
}
