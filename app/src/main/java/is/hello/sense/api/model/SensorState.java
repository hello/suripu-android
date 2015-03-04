package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitSystem;

public class SensorState extends ApiResponse {
    @JsonProperty("value")
    private Long value;

    @JsonProperty("message")
    private String message;

    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("last_updated_utc")
    private DateTime lastUpdated;

    @JsonProperty("ideal_conditions")
    private String idealConditions;


    public SensorState() {
    }

    @JsonIgnore
    public SensorState(long value,
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

    public Long getValue() {
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

    public @Nullable CharSequence getFormattedValue(@Nullable UnitSystem.Formatter formatter) {
        if (getValue() == null) {
            return null;
        } else if (formatter != null) {
            return formatter.format(getValue());
        } else {
            return Styles.assembleReadingAndUnit(getValue(), getUnit());
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
