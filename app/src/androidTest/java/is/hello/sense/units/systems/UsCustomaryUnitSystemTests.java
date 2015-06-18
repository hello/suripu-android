package is.hello.sense.units.systems;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsCustomaryUnitSystemTests {
    private final UsCustomaryUnitSystem unitSystem = new UsCustomaryUnitSystem();

    @Test
    public void formatMass() throws Exception {
        assertEquals("6 lbs", unitSystem.formatMass(2500).toString());
    }

    @Test
    public void formatTemperature() throws Exception {
        assertEquals("39 °", unitSystem.formatTemperature(4).toString());
    }

    @Test
    public void formatHeight() throws Exception {
        assertEquals("8' 2''", unitSystem.formatHeight(250).toString());
    }
}
