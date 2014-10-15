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


    //region Formatting

    abstract public String formatMass(long mass);
    abstract public String formatTemperature(long temperature);
    abstract public String formatHeight(long distance);

    //endregion
}
