package is.hello.sense.units.systems;

import android.support.annotation.NonNull;

import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;

public class UsCustomaryUnitSystem extends UnitSystem {
    public static final String NAME = "UsCustomary";

    @NonNull
    @Override
    public String getMassSuffix() {
        return " lbs";
    }

    @Override
    public long convertGrams(long mass) {
        return UnitOperations.gramsToPounds(mass);
    }

    @NonNull
    @Override
    public String getHeightSuffix() {
        return "";
    }

    @Override
    public long convertCentimeters(long distance) {
        return UnitOperations.centimetersToInches(distance);
    }

    @NonNull
    @Override
    public String getTemperatureSuffix() {
        return "ºF";
    }

    @Override
    public long convertDegreesCelsius(long temperature) {
        return UnitOperations.celsiusToFahrenheit(temperature);
    }
}
