package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.Condition;

public class Sensor implements Serializable {
    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private SensorType type;

    @SerializedName("unit")
    private Unit unit;

    @SerializedName("message")
    private String message;

    @SerializedName("condition")
    private Condition condition;

    @SerializedName("value")
    private double value;

    @SerializedName("scale")
    private List<Scale> scale;


    @Override
    public String toString() {
        return "Sensor{" +
                "Name=" + name +
                ", SensorType=" + type.toString() +
                ", Unit=" + unit.toString() +
                ", Message=" + message +
                ", Condition=" + condition +
                ", Value=" + value +
                "}";
    }


    public enum Unit implements Enums.FromString {
        CELCIUS,
        FAHRENHEIT,
        MG_CM,
        PERCENT,
        LUX,
        DB,
        VOC,
        PPM,
        RATIO,
        KELVIN,
        KPA;

        public static Unit fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), CELCIUS);
        }
    }

    public enum SensorType implements Enums.FromString {
        ALL,
        TEMP,
        HUMIDITY,
        AIR,
        LIGHT,
        SOUND,
        VOC,
        CO2,
        UV,
        LIGHT_TEMP,
        PRESSURE;

        public static SensorType fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), ALL);
        }
    }

    public enum Scope implements Enums.FromString {
        DAY,
        WEEK;

        public static Scope fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), DAY);
        }
    }

}
