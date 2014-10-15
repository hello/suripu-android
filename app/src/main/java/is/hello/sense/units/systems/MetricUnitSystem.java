package is.hello.sense.units.systems;

import android.support.annotation.NonNull;

import is.hello.sense.units.UnitSystem;

public class MetricUnitSystem extends UnitSystem {
    public static final String NAME = "Metric";

    @NonNull
    @Override
    public String getMassSuffix() {
        return "g";
    }

    @Override
    public long convertGrams(long mass) {
        return mass;
    }

    @NonNull
    @Override
    public String getHeightSuffix() {
        return "cm";
    }

    @Override
    public long convertCentimeters(long distance) {
        return distance;
    }

    @NonNull
    @Override
    public String getTemperatureSuffix() {
        return "ºC";
    }

    @Override
    public long convertDegreesCelsius(long temperature) {
        return temperature;
    }
}
