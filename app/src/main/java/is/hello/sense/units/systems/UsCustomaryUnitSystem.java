package is.hello.sense.units.systems;

import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;

public class UsCustomaryUnitSystem extends UnitSystem {
    public static final String NAME = "UsCustomary";

    @Override
    public String formatMass(float mass) {
        return UnitOperations.gramsToPounds((long) mass) + " lbs";
    }

    @Override
    public String formatTemperature(float temperature) {
        return UnitOperations.celsiusToFahrenheit((long) temperature) + "ºF";
    }

    @Override
    public String formatHeight(float distance) {
        long totalInches = UnitOperations.centimetersToInches((long) distance);
        long feet = totalInches / 12;
        long remainingInches = totalInches % 12;
        if (remainingInches > 0) {
            return feet + "' " + remainingInches + "''";
        } else {
            return feet + "'";
        }
    }
}
