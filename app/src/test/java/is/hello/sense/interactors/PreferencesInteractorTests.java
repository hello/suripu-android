package is.hello.sense.interactors;

import android.annotation.SuppressLint;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Sync;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@SuppressLint("CommitPrefEdits")
public class PreferencesInteractorTests extends InjectionTestCase {
    private static final String TEST_KEY = "test_key";

    @Inject
    PreferencesInteractor presenter;

    @Before
    public void setUp() throws Exception {
        presenter.edit()
                 .clear()
                 .commit();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void migrationForMetric() throws Exception {
        presenter.edit()
                 .putInt(PreferencesInteractor.SCHEMA_VERSION, PreferencesInteractor.SCHEMA_VERSION_1_0)
                 .putString(PreferencesInteractor.UNIT_SYSTEM__LEGACY, UnitFormatter.LEGACY_UNIT_SYSTEM_METRIC)
                 .commit();

        assertThat(presenter.migrateIfNeeded(), is(true));
        assertThat(presenter.contains(PreferencesInteractor.UNIT_SYSTEM__LEGACY), is(false));
        assertThat(presenter.getBoolean(PreferencesInteractor.USE_CELSIUS, false), is(true));
        assertThat(presenter.getBoolean(PreferencesInteractor.USE_GRAMS, false), is(true));
        assertThat(presenter.getBoolean(PreferencesInteractor.USE_CENTIMETERS, false), is(true));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void migrationForUsCustomary() throws Exception {
        presenter.edit()
                 .putInt(PreferencesInteractor.SCHEMA_VERSION, PreferencesInteractor.SCHEMA_VERSION_1_0)
                 .putString(PreferencesInteractor.UNIT_SYSTEM__LEGACY, UnitFormatter.LEGACY_UNIT_SYSTEM_US_CUSTOMARY)
                 .commit();

        assertThat(presenter.migrateIfNeeded(), is(true));
        assertThat(presenter.contains(PreferencesInteractor.UNIT_SYSTEM__LEGACY), is(false));
        assertThat(presenter.getBoolean(PreferencesInteractor.USE_CELSIUS, true), is(false));
        assertThat(presenter.getBoolean(PreferencesInteractor.USE_GRAMS, true), is(false));
        assertThat(presenter.getBoolean(PreferencesInteractor.USE_CENTIMETERS, true), is(false));
        assertThat(presenter.getInt(PreferencesInteractor.SCHEMA_VERSION, PreferencesInteractor.SCHEMA_VERSION_1_0),
                   is(PreferencesInteractor.SCHEMA_VERSION_1_1));
    }

    @Test
    public void observeChangesOn() throws Exception {
        Set<String> changes = new HashSet<>();

        presenter.observeChangesOn(PreferencesInteractor.USE_CELSIUS,
                                   PreferencesInteractor.USE_CENTIMETERS)
                 .subscribe(changes::add);

        presenter.edit()
                 .putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                 .commit();

        presenter.edit()
                 .putBoolean(PreferencesInteractor.USE_CENTIMETERS, true)
                 .commit();

        presenter.edit()
                 .putBoolean(PreferencesInteractor.USE_24_TIME, true)
                 .commit();

        assertThat(changes.size(), is(equalTo(2)));
        assertThat(changes, hasItem(PreferencesInteractor.USE_CELSIUS));
        assertThat(changes, hasItem(PreferencesInteractor.USE_CENTIMETERS));
        assertThat(changes, not(hasItem(PreferencesInteractor.USE_24_TIME)));
    }

    @Test
    public void observableString() throws Exception {
        Sync.wrap(presenter.observableString(TEST_KEY, "placeholder").take(1))
            .assertThat(is(equalTo("placeholder")));

        presenter.edit()
                 .putString(TEST_KEY, "actual value")
                 .commit();

        Sync.wrap(presenter.observableString(TEST_KEY, "placeholder").take(1))
            .assertThat(is(equalTo("actual value")));
    }

    @Test
    public void observableBoolean() throws Exception {
        Sync.wrap(presenter.observableBoolean(TEST_KEY, true).take(1))
            .assertThat(is(true));

        presenter.edit()
                 .putBoolean(TEST_KEY, false)
                 .commit();

        Sync.wrap(presenter.observableBoolean(TEST_KEY, true).take(1))
            .assertThat(is(false));
    }

    @Test
    public void observableInteger() throws Exception {
        Sync.wrap(presenter.observableInteger(TEST_KEY, 10).take(1))
            .assertThat(is(equalTo(10)));

        presenter.edit()
                 .putInt(TEST_KEY, 99)
                 .commit();

        Sync.wrap(presenter.observableInteger(TEST_KEY, 10).take(1))
            .assertThat(is(equalTo(99)));
    }

    @Test
    public void localDates() throws Exception {
        final LocalDate testDate = new LocalDate(2015, 9, 8);

        assertThat(presenter.getLocalDate(TEST_KEY), is(nullValue()));

        presenter.putLocalDate(TEST_KEY, testDate);
        Robolectric.flushBackgroundThreadScheduler();

        assertThat(presenter.getLocalDate(TEST_KEY), is(equalTo(testDate)));
    }
}
