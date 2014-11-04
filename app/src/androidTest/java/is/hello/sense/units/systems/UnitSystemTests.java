package is.hello.sense.units.systems;

import junit.framework.TestCase;

import java.util.Locale;
import java.util.Map;

import is.hello.sense.units.UnitSystem;

public class UnitSystemTests extends TestCase {
    public void testAllSystemsPresent() throws Exception {
        Map<String, Class<? extends UnitSystem>> systems = UnitSystem.getUnitSystems();
        assertTrue(systems.values().contains(UsCustomaryUnitSystem.class));
        assertTrue(systems.values().contains(MetricUnitSystem.class));
    }

    public void testGetDefaultUnitSystem() {
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getDefaultUnitSystem(Locale.US));
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getDefaultUnitSystem(new Locale("mya", "MM")));
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getDefaultUnitSystem(new Locale("en", "LR")));

        assertEquals(MetricUnitSystem.NAME, UnitSystem.getDefaultUnitSystem(Locale.CANADA));
        assertEquals(MetricUnitSystem.NAME, UnitSystem.getDefaultUnitSystem(Locale.UK));
        assertEquals(MetricUnitSystem.NAME, UnitSystem.getDefaultUnitSystem(Locale.FRANCE));
    }

    public void testFormatParticulates() throws Exception {
        UnitSystem system = new UnitSystem();
        assertEquals("0.000", system.formatParticulates(0f));
        assertEquals("0.004", system.formatParticulates(0.004f));
        assertEquals("0.650", system.formatParticulates(0.65f));
    }
}
