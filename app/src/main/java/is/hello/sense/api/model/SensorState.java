package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import is.hello.sense.units.UnitFormatter;

public class SensorState extends ApiResponse {
    @JsonProperty("value")
    private Float value;

    @JsonProperty("message")
    private String message;

    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("last_updated_utc")
    private DateTime lastUpdated;


    public SensorState() {
    }

    @JsonIgnore
    public SensorState(float value,
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

    public Float getValue() {
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

    public @Nullable String getFormattedValue(@Nullable UnitFormatter.Formatter formatter) {
        if (getValue() == null) {
            return null;
        } else if (formatter != null) {
            return formatter.format(getValue());
        } else {
            return getValue().longValue() + getUnit();
        }
    }

    @Override
    public String toString() {
        return "SensorState{" +
                "value=" + value +
                ", message='" + message + '\'' +
                ", condition=" + condition +
                ", unit=" + unit +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
