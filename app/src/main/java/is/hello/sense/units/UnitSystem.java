package is.hello.sense.units;

import android.support.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;

/**
 * An object that converts raw SI units to human readable values.
 */
public class UnitSystem {
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


    //region Formatting

    public String formatMass(float mass) {
        return (long) (Math.round(mass)) + "g";
    }

    public String formatTemperature(float temperature) {
        return (long) (Math.round(temperature)) + "ºC";
    }

    public String formatHeight(float distance) {
        return (long) (Math.round(distance)) + "cm";
    }

    public String formatParticulates(float particulates) {
        return String.format("%.3fµg/m³", particulates);
    }

    //endregion
}
