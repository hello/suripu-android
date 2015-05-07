package is.hello.sense.units.systems;

import is.hello.sense.api.ApiService;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;

public class UsCustomaryUnitSystem extends UnitSystem {
    public static final String NAME = "UsCustomary";

    //region Mass

    @Override
    public String getMassUnit() {
        return "lbs";
    }

    @Override
    public long convertMass(long mass) {
        return UnitOperations.gramsToPounds((int) mass);
    }

    //endregion


    //region Temperature

    @Override
    public String getApiTemperatureUnit() {
        return ApiService.UNIT_TEMPERATURE_US_CUSTOMARY;
    }
    @Override
    public long convertTemperature(long temperature) {
        return UnitOperations.celsiusToFahrenheit(temperature);
    }

    //endregion


    //region Height

    @Override
    public String getHeightUnit() {
        return "";
    }

    @Override
    public CharSequence formatHeight(long distance) {
        long totalInches = UnitOperations.centimetersToInches(distance);
        long feet = totalInches / 12;
        long remainingInches = totalInches % 12;
        if (remainingInches > 0) {
            return feet + "' " + remainingInches + "''";
        } else {
            return feet + "'";
        }
    }

    //endregion
}
