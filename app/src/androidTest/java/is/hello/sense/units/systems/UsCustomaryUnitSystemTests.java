package is.hello.sense.units.systems;

import junit.framework.TestCase;

public class UsCustomaryUnitSystemTests extends TestCase {
    private final UsCustomaryUnitSystem unitSystem = new UsCustomaryUnitSystem();

    public void testConvertGrams() throws Exception {
        assertEquals(6, unitSystem.convertGrams(2500));
    }

    public void testConvertCentimeters() throws Exception {
        assertEquals(98, unitSystem.convertCentimeters(250));
    }

    public void testConvertDegreesCelsius() throws Exception {
        assertEquals(39, unitSystem.convertDegreesCelsius(4));
    }
}
