package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;

import org.junit.Before;
import org.junit.Test;

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
    public void migrationForMetric() throws Exception {
        presenter.edit()
                 .putInt(PreferencesPresenter.SCHEMA_VERSION, PreferencesPresenter.SCHEMA_VERSION_1_0)
                 .putString(PreferencesPresenter.UNIT_SYSTEM__LEGACY, UnitFormatter.LEGACY_UNIT_SYSTEM_METRIC)
                 .commit();

        assertThat(presenter.migrateIfNeeded(), is(true));
        assertThat(presenter.contains(PreferencesPresenter.UNIT_SYSTEM__LEGACY), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CELSIUS, false), is(true));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_GRAMS, false), is(true));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CENTIMETERS, false), is(true));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void migrationForUsCustomary() throws Exception {
        presenter.edit()
                 .putInt(PreferencesPresenter.SCHEMA_VERSION, PreferencesPresenter.SCHEMA_VERSION_1_0)
                 .putString(PreferencesPresenter.UNIT_SYSTEM__LEGACY, UnitFormatter.LEGACY_UNIT_SYSTEM_US_CUSTOMARY)
                 .commit();

        assertThat(presenter.migrateIfNeeded(), is(true));
        assertThat(presenter.contains(PreferencesPresenter.UNIT_SYSTEM__LEGACY), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CELSIUS, true), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_GRAMS, true), is(false));
        assertThat(presenter.getBoolean(PreferencesPresenter.USE_CENTIMETERS, true), is(false));
        assertThat(presenter.getInt(PreferencesPresenter.SCHEMA_VERSION, PreferencesPresenter.SCHEMA_VERSION_1_0),
                   is(PreferencesPresenter.SCHEMA_VERSION_1_1));
    }

    @Test
    public void observeChangesOn() throws Exception {
        Set<String> changes = new HashSet<>();

        presenter.observeChangesOn(PreferencesPresenter.USE_CELSIUS,
                                   PreferencesPresenter.USE_CENTIMETERS)
                 .subscribe(changes::add);

        presenter.edit()
                 .putBoolean(PreferencesPresenter.USE_CELSIUS, true)
                 .commit();

        presenter.edit()
                 .putBoolean(PreferencesPresenter.USE_CENTIMETERS, true)
                 .commit();

        presenter.edit()
                 .putBoolean(PreferencesPresenter.USE_24_TIME, true)
                 .commit();

        assertThat(changes.size(), is(equalTo(2)));
        assertThat(changes, hasItem(PreferencesPresenter.USE_CELSIUS));
        assertThat(changes, hasItem(PreferencesPresenter.USE_CENTIMETERS));
        assertThat(changes, not(hasItem(PreferencesPresenter.USE_24_TIME)));
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
}
