package is.hello.sense.units.systems;

import junit.framework.TestCase;

public class MetricUnitSystemTests extends TestCase {
    private final MetricUnitSystem unitSystem = new MetricUnitSystem();

    public void testFormatMass() throws Exception {
        assertEquals("72 kg", unitSystem.formatMass(72000).toString());
    }

    public void testFormatTemperature() throws Exception {
        assertEquals("20 °", unitSystem.formatTemperature(20).toString());
    }

    public void testFormatHeight() throws Exception {
        assertEquals("400 cm", unitSystem.formatHeight(400).toString());
    }
}
