package is.hello.sense.units;

import android.annotation.SuppressLint;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.util.Sync;

public class UnitFormatterTests extends InjectionTestCase {
    @Inject UnitFormatter formatter;
    @Inject PreferencesPresenter preferences;

    private UnitSystem unitSystem;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        preferences.edit()
                   .remove(PreferencesPresenter.UNIT_SYSTEM)
                   .commit();

        Sync<UnitSystem> unitSystem = Sync.of(1, formatter.unitSystem);
        this.unitSystem = unitSystem.last();
    }

    public void testFormatMass() throws Exception {
        CharSequence formattedMass = unitSystem.formatMass(2500);
        assertNotNull(formattedMass);
        assertEquals("6lbs", formattedMass.toString());
    }

    public void testFormatTemperature() throws Exception {
        CharSequence formattedTemperature = unitSystem.formatTemperature(4);
        assertNotNull(formattedTemperature);
        assertEquals("39º", formattedTemperature.toString());
    }

    public void testFormatDistance() throws Exception {
        CharSequence formattedDistance = unitSystem.formatHeight(250);
        assertNotNull(formattedDistance);
        assertEquals("8' 2''", formattedDistance.toString());
    }
}
