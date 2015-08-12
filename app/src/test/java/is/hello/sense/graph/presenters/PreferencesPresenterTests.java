package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;
import is.hello.sense.util.Sync;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressLint("CommitPrefEdits")
public class PreferencesPresenterTests extends InjectionTestCase {
    private static final String TEST_KEY = "test_key";

    @Inject PreferencesPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter.edit()
                 .clear()
                 .commit();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void migrationForMetric() {
        presenter.edit()
                 .putInt(PreferencesPresenter.SCHEMA_VERSION, PreferencesPresenter.SCHEMA_VERSION_1_0)
                 .putString(PreferencesPresenter.UNIT_SYSTEM__LEGACY, MetricUnitSystem.NAME)
                 .commit();

        assertThat(presenter.migrateIfNeeded(), is(true));
        assertThat(presenter.contains(PreferencesPresenter.UNIT_SYSTEM__LEGACY), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CELSIUS, false), is(true));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_GRAMS, false), is(true));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CENTIMETERS, false), is(true));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void migrationForUsCustomary() {
        presenter.edit()
                 .putInt(PreferencesPresenter.SCHEMA_VERSION, PreferencesPresenter.SCHEMA_VERSION_1_0)
                 .putString(PreferencesPresenter.UNIT_SYSTEM__LEGACY, UsCustomaryUnitSystem.NAME)
                 .commit();

        assertThat(presenter.migrateIfNeeded(), is(true));
        assertThat(presenter.contains(PreferencesPresenter.UNIT_SYSTEM__LEGACY), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CELSIUS, true), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_GRAMS, true), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CENTIMETERS, true), is(false));
    }

    @Test
    public void observableString() throws Exception {
        Sync.wrap(presenter.observableString(TEST_KEY, "placeholder").take(1))
            .assertEquals("placeholder");

        presenter.edit()
                 .putString(TEST_KEY, "actual value")
                 .commit();

        Sync.wrap(presenter.observableString(TEST_KEY, "placeholder").take(1))
            .assertEquals("actual value");
    }

    @Test
    public void observableBoolean() throws Exception {
        Sync.wrap(presenter.observableBoolean(TEST_KEY, true).take(1))
            .assertEquals(true);

        presenter.edit()
                 .putBoolean(TEST_KEY, false)
                 .commit();

        Sync.wrap(presenter.observableBoolean(TEST_KEY, true).take(1))
            .assertEquals(false);
    }

    @Test
    public void observableInteger() throws Exception {
        Sync.wrap(presenter.observableInteger(TEST_KEY, 10).take(1))
            .assertEquals(10);

        presenter.edit()
                .putInt(TEST_KEY, 99)
                .commit();

        Sync.wrap(presenter.observableInteger(TEST_KEY, 10).take(1))
            .assertEquals(99);
    }

    @Test
    public void observableLong() throws Exception {
        Sync.wrap(presenter.observableLong(TEST_KEY, 10).take(1))
            .assertEquals(10L);

        presenter.edit()
                .putLong(TEST_KEY, 99)
                .commit();

        Sync.wrap(presenter.observableLong(TEST_KEY, 10).take(1))
            .assertEquals(99L);
    }

    @Test
    public void observableFloat() throws Exception {
        Sync.wrap(presenter.observableFloat(TEST_KEY, 4f).take(1))
            .assertEquals(4f);

        presenter.edit()
                .putFloat(TEST_KEY, 8f)
                .commit();

        Sync.wrap(presenter.observableFloat(TEST_KEY, 4f).take(1))
            .assertEquals(8f);
    }
}
