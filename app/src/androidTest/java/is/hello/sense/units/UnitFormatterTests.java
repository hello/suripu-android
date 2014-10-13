package is.hello.sense.units;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Constants;

public class UnitFormatterTests extends InjectionTestCase {
    @Inject UnitFormatter formatter;
    @Inject SharedPreferences sharedPreferences;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        sharedPreferences.edit()
                .remove(Constants.GLOBAL_PREF_UNIT_SYSTEM)
                .commit();
    }

    public void testFormatMass() throws Exception {
        String formattedMass = formatter.formatMass(2500);
        assertNotNull(formattedMass);
        assertEquals("6 lbs", formattedMass);
    }

    public void testFormatTemperature() throws Exception {
        String formattedTemperature = formatter.formatTemperature(4);
        assertNotNull(formattedTemperature);
        assertEquals("39ºF", formattedTemperature);
    }

    public void testFormatDistance() throws Exception {
        String formattedDistance = formatter.formatDistance(250);
        assertNotNull(formattedDistance);
        assertEquals("98 in", formattedDistance);
    }

    public void testRaw() throws Exception {
        String formattedRaw = formatter.formatRaw(2500);
        assertNotNull(formattedRaw);
        assertEquals("2500", formattedRaw);
    }
}
