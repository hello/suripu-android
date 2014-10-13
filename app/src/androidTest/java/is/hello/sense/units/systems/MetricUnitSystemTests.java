package is.hello.sense.units.systems;

import junit.framework.TestCase;

public class MetricUnitSystemTests extends TestCase {
    private final MetricUnitSystem unitSystem = new MetricUnitSystem();

    public void testConvertGrams() throws Exception {
        assertEquals(2500, unitSystem.convertGrams(2500));
    }

    public void testConvertCentimeters() throws Exception {
        assertEquals(250, unitSystem.convertCentimeters(250));
    }

    public void testConvertDegreesCelsius() throws Exception {
        assertEquals(4, unitSystem.convertDegreesCelsius(4));
    }
}
