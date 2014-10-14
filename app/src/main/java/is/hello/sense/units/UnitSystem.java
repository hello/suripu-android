package is.hello.sense.units;

import android.support.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;

/**
 * An object that converts raw SI units to human readable values.
 */
public abstract class UnitSystem {
    //region Systems

    private static final LinkedHashMap<String, Class<? extends UnitSystem>> UNIT_SYSTEMS = new LinkedHashMap<>();

    public static final String DEFAULT_UNIT_SYSTEM = UsCustomaryUnitSystem.NAME;
    public static Map<String, Class<? extends UnitSystem>> getUnitSystems() {
        if (UNIT_SYSTEMS.isEmpty()) {
            UNIT_SYSTEMS.put(MetricUnitSystem.NAME, MetricUnitSystem.class);
            UNIT_SYSTEMS.put(UsCustomaryUnitSystem.NAME, UsCustomaryUnitSystem.class);
        }

        return UNIT_SYSTEMS;
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

    //region Primitives

    protected abstract @NonNull String getMassSuffix();
    protected abstract long convertGrams(long mass);

    protected abstract @NonNull String getDistanceSuffix();
    protected abstract long convertCentimeters(long distance);

    protected abstract @NonNull String getTemperatureSuffix();
    protected abstract long convertDegreesCelsius(long temperature);

    //endregion


    //region Formatting

    protected String assemble(long value, String unit) {
        return value + unit;
    }

    public String formatMass(long mass) {
        return assemble(convertGrams(mass), getMassSuffix());
    }

    public String formatTemperature(long temperature) {
        return assemble(convertDegreesCelsius(temperature), getTemperatureSuffix());
    }

    public String formatDistance(long distance) {
        return assemble(convertCentimeters(distance), getDistanceSuffix());
    }

    //endregion
}
