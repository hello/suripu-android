package is.hello.sense.units.systems;

import org.junit.Test;

import java.util.Locale;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.units.UnitSystem;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
public class UnitSystemTests extends SenseTestCase {
    private final UnitSystem testSystem = new UnitSystem();

    @Test
    public void getLocaleUnitSystemName() throws Exception {
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.US));
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(new Locale("mya", "MM")));
        assertEquals(UsCustomaryUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(new Locale("en", "LR")));

        assertEquals(MetricUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.CANADA));
        assertEquals(MetricUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.UK));
        assertEquals(MetricUnitSystem.NAME, UnitSystem.getLocaleUnitSystemName(Locale.FRANCE));
    }

    @Test
    public void formatMass() throws Exception {
        assertEquals("72 kg", testSystem.formatMass(72000).toString());
    }

    @Test
    public void formatHumidity() throws Exception {
        assertEquals("50 %", testSystem.formatHumidity(50).toString());
        assertEquals("75 %", testSystem.formatHumidity(75).toString());
        assertEquals("100 %", testSystem.formatHumidity(100).toString());
    }

    @Test
    public void formatDecibels() throws Exception {
        assertEquals("1 db", testSystem.formatSound(1).toString());
        assertEquals("5 db", testSystem.formatSound(5).toString());
        assertEquals("10 db", testSystem.formatSound(10).toString());
    }

    @Test
    public void formatLight() throws Exception {
        assertEquals("5 lux", testSystem.formatLight(5).toString());
        assertEquals("50 lux", testSystem.formatLight(50).toString());
        assertEquals("100 lux", testSystem.formatLight(100).toString());
    }

    @Test
    public void formatParticulates() throws Exception {
        assertEquals("200", testSystem.formatParticulates(200).toString());
        assertEquals("40", testSystem.formatParticulates(40).toString());
        assertEquals("65", testSystem.formatParticulates(65).toString());
    }
}
