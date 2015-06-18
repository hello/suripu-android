package is.hello.sense.units.systems;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetricUnitSystemTests {
    private final MetricUnitSystem unitSystem = new MetricUnitSystem();

    @Test
    public void formatMass() throws Exception {
        assertEquals("72 kg", unitSystem.formatMass(72000).toString());
    }

    @Test
    public void formatTemperature() throws Exception {
        assertEquals("20 °", unitSystem.formatTemperature(20).toString());
    }

    @Test
    public void formatHeight() throws Exception {
        assertEquals("400 cm", unitSystem.formatHeight(400).toString());
    }
}
