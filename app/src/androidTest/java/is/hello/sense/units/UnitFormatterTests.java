package is.hello.sense.units;

import android.annotation.SuppressLint;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.util.SyncObserver;

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

        SyncObserver<UnitSystem> unitSystemObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, formatter.unitSystem);
        unitSystemObserver.await();
        this.unitSystem = unitSystemObserver.getSingle();
    }

    public void testFormatMass() throws Exception {
        String formattedMass = unitSystem.formatMass(2500);
        assertNotNull(formattedMass);
        assertEquals("6 lbs", formattedMass);
    }

    public void testFormatTemperature() throws Exception {
        String formattedTemperature = unitSystem.formatTemperature(4);
        assertNotNull(formattedTemperature);
        assertEquals("39º", formattedTemperature);
    }

    public void testFormatDistance() throws Exception {
        String formattedDistance = unitSystem.formatHeight(250);
        assertNotNull(formattedDistance);
        assertEquals("8' 2''", formattedDistance);
    }
}
