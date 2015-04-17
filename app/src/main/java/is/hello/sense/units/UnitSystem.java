package is.hello.sense.units;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import is.hello.sense.api.ApiService;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;

/**
 * An object that converts raw SI units to human readable values.
 */
public class UnitSystem implements Serializable {
    public static final String TEMP_SUFFIX = "°";

    //region Vending Systems

    public static @NonNull String getLocaleUnitSystemName(@NonNull Locale locale) {
        String countryCode = locale.getCountry();
        if ("US".equals(countryCode) || "LR".equals(countryCode) || "MM".equals(countryCode)) {
            return UsCustomaryUnitSystem.NAME;
        } else {
            return MetricUnitSystem.NAME;
        }
    }

    public static @NonNull UnitSystem createUnitSystemWithName(@NonNull String name) {
        switch (name) {
            case MetricUnitSystem.NAME: {
                return new MetricUnitSystem();
            }

            case UsCustomaryUnitSystem.NAME: {
                return new UsCustomaryUnitSystem();
            }

            default: {
                throw new IllegalStateException("unknown unit system with name: " + name);
            }
        }
    }

    //endregion


    public String getApiTemperatureUnit() {
        return ApiService.UNIT_TEMPERATURE_CELSIUS;
    }


    //region Formatting

    public String getMassUnit() {
        return "g";
    }

    public CharSequence formatMass(long mass) {
        return Styles.assembleReadingAndUnit(mass, getMassUnit());
    }

    public String getTemperatureUnit() {
        return TEMP_SUFFIX;
    }

    public CharSequence formatTemperature(long temperature) {
        return Styles.assembleReadingAndUnit(temperature, getTemperatureUnit());
    }

    public String getHumidityUnit() {
        return "%";
    }

    public CharSequence formatHumidity(long humidity) {
        return Styles.assembleReadingAndUnit(humidity, getHumidityUnit());
    }

    public String getHeightUnit() {
        return "cm";
    }

    public CharSequence formatHeight(long distance) {
        return Styles.assembleReadingAndUnit(distance, getHeightUnit());
    }

    public String getSoundUnit() {
        return "db";
    }

    public CharSequence formatDecibels(long decibels) {
        return Styles.assembleReadingAndUnit(decibels, getSoundUnit());
    }

    public String getLightUnit() {
        return "lux";
    }

    public CharSequence formatLight(long lux) {
        return Styles.assembleReadingAndUnit(lux, getLightUnit());
    }

    public String getParticulatesUnit() {
        return "";
    }

    public CharSequence formatParticulates(long particulates) {
        return Long.toString(particulates);
    }

    public @Nullable Formatter getUnitFormatterForSensor(@NonNull String sensor) {
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE:
                return this::formatTemperature;

            case ApiService.SENSOR_NAME_PARTICULATES:
                return this::formatParticulates;

            default:
                return null;
        }
    }

    public List<String> getUnitNamesAsList() {
        return Arrays.asList(getTemperatureUnit(), getHumidityUnit(), getLightUnit(), getSoundUnit());
    }

    public interface Formatter {
        @NonNull CharSequence format(Long value);
    }

    //endregion
}
