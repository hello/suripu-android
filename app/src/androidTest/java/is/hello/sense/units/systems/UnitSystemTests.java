package is.hello.sense.units.systems;

import junit.framework.TestCase;

import java.util.Map;

import is.hello.sense.units.UnitSystem;

public class UnitSystemTests extends TestCase {
    public void testAllSystemsPresent() throws Exception {
        Map<String, Class<? extends UnitSystem>> systems = UnitSystem.getUnitSystems();
        assertTrue(systems.values().contains(UsCustomaryUnitSystem.class));
        assertTrue(systems.values().contains(MetricUnitSystem.class));
    }
}
