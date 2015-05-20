package is.hello.sense.units.systems;

import junit.framework.TestCase;

import java.util.Locale;

import is.hello.sense.units.UnitSystem;

@SuppressWarnings("deprecation")
public class UnitSystemTests extends TestCase {
    private final UnitSystem testSystem = new UnitSystem();

    public void testGetLocaleUnitSystemName() throws Exception {
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.US));
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(new Locale("mya", "MM")));
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(new Locale("en", "LR")));

        assertEquals(MetricUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.CANADA));
        assertEquals(MetricUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.UK));
        assertEquals(MetricUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.FRANCE));
    }

    public void testFormatMass() throws Exception {
        assertEquals("72 kg", testSystem.formatMass(72000).toString());
    }

    public void testFormatHumidity() throws Exception {
        assertEquals("50 %", testSystem.formatHumidity(50).toString());
        assertEquals("75 %", testSystem.formatHumidity(75).toString());
        assertEquals("100 %", testSystem.formatHumidity(100).toString());
    }

    public void testFormatDecibels() throws Exception {
        assertEquals("1 db", testSystem.formatDecibels(1).toString());
        assertEquals("5 db", testSystem.formatDecibels(5).toString());
        assertEquals("10 db", testSystem.formatDecibels(10).toString());
    }

    public void testFormatLight() throws Exception {
        assertEquals("5 lux", testSystem.formatLight(5).toString());
        assertEquals("50 lux", testSystem.formatLight(50).toString());
        assertEquals("100 lux", testSystem.formatLight(100).toString());
    }

    public void testFormatParticulates() throws Exception {
        assertEquals("200", testSystem.formatParticulates(200).toString());
        assertEquals("40", testSystem.formatParticulates(40).toString());
        assertEquals("65", testSystem.formatParticulates(65).toString());
    }
}
