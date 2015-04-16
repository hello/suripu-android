package is.hello.sense.units.systems;

import is.hello.sense.api.ApiService;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;

public class UsCustomaryUnitSystem extends UnitSystem {
    public static final String NAME = "UsCustomary";

    @Override
    public String getApiTemperatureUnit() {
        return ApiService.UNIT_TEMPERATURE_US_CUSTOMARY;
    }

    @Override
    public String getMassUnit() {
        return "lbs";
    }

    @Override
    public CharSequence formatMass(long mass) {
        return Styles.assembleReadingAndUnit(UnitOperations.gramsToPounds((int) mass), getMassUnit());
    }

    @Override
    public CharSequence formatTemperature(long temperature) {
        return Styles.assembleReadingAndUnit(UnitOperations.celsiusToFahrenheit(temperature), getTemperatureUnit());
    }

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
}
