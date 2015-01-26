package is.hello.sense.units.systems;

import junit.framework.TestCase;

public class UsCustomaryUnitSystemTests extends TestCase {
    private final UsCustomaryUnitSystem unitSystem = new UsCustomaryUnitSystem();

    public void testFormatMass() throws Exception {
        assertEquals("6lbs", unitSystem.formatMass(2500).toString());
    }

    public void testFormatTemperature() throws Exception {
        assertEquals("39º", unitSystem.formatTemperature(4).toString());
    }

    public void testFormatHeight() throws Exception {
        assertEquals("8' 2''", unitSystem.formatHeight(250).toString());
    }
}
