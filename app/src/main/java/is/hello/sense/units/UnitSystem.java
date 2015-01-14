package is.hello.sense.units;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;

/**
 * An object that converts raw SI units to human readable values.
 */
public class UnitSystem implements Serializable {
    //region Systems

    private static final LinkedHashMap<String, Class<? extends UnitSystem>> UNIT_SYSTEMS = new LinkedHashMap<>();

    public static Map<String, Class<? extends UnitSystem>> getUnitSystems() {
        if (UNIT_SYSTEMS.isEmpty()) {
            UNIT_SYSTEMS.put(MetricUnitSystem.NAME, MetricUnitSystem.class);
            UNIT_SYSTEMS.put(UsCustomaryUnitSystem.NAME, UsCustomaryUnitSystem.class);
        }

        return UNIT_SYSTEMS;
    }

    public static @NonNull String getDefaultUnitSystem(@NonNull Locale locale) {
        String countryCode = locale.getCountry();
        if ("US".equals(countryCode) || "LR".equals(countryCode) || "MM".equals(countryCode)) {
            return UsCustomaryUnitSystem.NAME;
        } else {
            return MetricUnitSystem.NAME;
        }
    }

    public static @NonNull UnitSystem createUnitSystemWithName(@NonNull String name) {
        Class<? extends UnitSystem> clazz = getUnitSystems().get(name);
        if (clazz == null)
            throw new IllegalStateException("unknown unit system with name: " + name);

        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    //endregion


    public String getApiTemperatureUnit() {
        return ApiService.UNIT_TEMPERATURE_CELCIUS;
    }


    //region Formatting

    public String formatMass(float mass) {
        return (long) (Math.round(mass)) + "g";
    }

    public String formatTemperature(float temperature) {
        return (long) (Math.round(temperature)) + "º";
    }

    public String formatHeight(float distance) {
        return (long) (Math.round(distance)) + "cm";
    }

    public String formatDecibels(float decibels) {
        return (long) (Math.round(decibels)) + "db";
    }

    public String formatLight(float lux) {
        return Integer.toString((int) lux);
    }

    public String formatParticulates(float particulates) {
        return Long.toString(Math.round(particulates));
    }

    public @Nullable UnitFormatter.Formatter getUnitFormatterForSensor(@NonNull String sensor) {
        switch (sensor) {
            case SensorHistory.SENSOR_NAME_TEMPERATURE:
                return this::formatTemperature;

            case SensorHistory.SENSOR_NAME_PARTICULATES:
                return this::formatParticulates;

            case SensorHistory.SENSOR_NAME_LIGHT:
                return this::formatLight;

            default:
                return null;
        }
    }

    //endregion
}
