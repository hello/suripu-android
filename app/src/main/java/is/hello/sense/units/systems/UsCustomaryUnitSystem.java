package is.hello.sense.units.systems;

import is.hello.sense.api.ApiService;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;

public class UsCustomaryUnitSystem extends UnitSystem {
    public static final String NAME = "UsCustomary";

    @Override
    public String getApiTemperatureUnit() {
        return ApiService.UNIT_TEMPERATURE_US_CUSTOMARY;
    }

    @Override
    public String formatMass(float mass) {
        return UnitOperations.gramsToPounds((long) mass) + " lbs";
    }

    @Override
    public String formatTemperature(float temperature) {
        return UnitOperations.celsiusToFahrenheit((long) temperature) + "º";
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
