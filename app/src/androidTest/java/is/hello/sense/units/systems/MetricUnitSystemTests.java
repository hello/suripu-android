package is.hello.sense.units.systems;

import junit.framework.TestCase;

public class MetricUnitSystemTests extends TestCase {
    private final MetricUnitSystem unitSystem = new MetricUnitSystem();

    public void testFormatMass() throws Exception {
        assertEquals("200g", unitSystem.formatMass(200));
    }

    public void testFormatTemperature() throws Exception {
        assertEquals("20ºC", unitSystem.formatTemperature(20));
    }

    public void testFormatHeight() throws Exception {
        assertEquals("400cm", unitSystem.formatHeight(400));
    }
}
