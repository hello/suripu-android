package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitPrinter;
import is.hello.sense.util.markup.text.MarkupString;

public class SensorState extends ApiResponse {
    @Expose(deserialize = false, serialize = false)
    private String name;

    @SerializedName("value")
    private Double value;

    @SerializedName("message")
    private MarkupString message;

    @SerializedName("condition")
    private Condition condition;

    @SerializedName("unit")
    private String unit;

    @SerializedName("last_updated_utc")
    private DateTime lastUpdated;

    @SerializedName("ideal_conditions")
    private MarkupString idealConditions;


    public SensorState() {
    }

    public SensorState(double value,
                       @NonNull MarkupString message,
                       @NonNull Condition condition,
                       @NonNull String unit,
                       @NonNull DateTime lastUpdated) {
        this.value = value;
        this.message = message;
        this.condition = condition;
        this.unit = unit;
        this.lastUpdated = lastUpdated;
    }

    /**
     * Called by {@link RoomConditions}.
     */
    void setName(@NonNull String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Double getValue() {
        return value;
    }

    public MarkupString getMessage() {
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

    public MarkupString getIdealConditions() {
        return idealConditions;
    }

    public @Nullable CharSequence getFormattedValue(@Nullable UnitPrinter printer) {
        if (getValue() == null) {
            return null;
        } else if (printer != null) {
            return printer.print(getValue());
        } else {
            return Styles.assembleReadingAndUnit(getValue(), getUnit());
        }
    }

    @Override
    public String toString() {
        return "SensorState{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", message='" + message + '\'' +
                ", condition=" + condition +
                ", unit='" + unit + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", idealConditions='" + idealConditions + '\'' +
                '}';
    }
}
