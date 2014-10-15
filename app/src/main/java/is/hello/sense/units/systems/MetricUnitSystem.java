package is.hello.sense.units.systems;

import is.hello.sense.units.UnitSystem;

public class MetricUnitSystem extends UnitSystem {
    public static final String NAME = "Metric";

    @Override
    public String formatMass(long mass) {
        return mass + "g";
    }

    @Override
    public String formatTemperature(long temperature) {
        return temperature + "ºC";
    }

    @Override
    public String formatHeight(long distance) {
        return distance + "cm";
    }
}
