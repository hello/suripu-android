package is.hello.sense.units.systems;

import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;

public class UsCustomaryUnitSystem extends UnitSystem {
    public static final String NAME = "UsCustomary";

    @Override
    public String formatMass(long mass) {
        return UnitOperations.gramsToPounds(mass) + " lbs";
    }

    @Override
    public String formatTemperature(long temperature) {
        return UnitOperations.celsiusToFahrenheit(temperature) + "ºF";
    }

    @Override
    public String formatHeight(long distance) {
        long totalInches = UnitOperations.centimetersToInches(distance);
        long feet = totalInches / 12;
        long remainingInches = totalInches % 12;
        if (remainingInches > 0) {
            return feet + "' " + remainingInches + "''";
        } else {
            return feet + "'";
        }
    }
}
