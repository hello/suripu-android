package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

@SuppressLint("CommitPrefEdits")
public class PreferencesPresenterTests extends InjectionTestCase {
    private static final String TEST_KEY = "test_key";

    @Inject PreferencesPresenter presenter;

    @Before
    public void initialize() throws Exception {
        presenter.edit()
                 .remove(TEST_KEY)
                 .commit();
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
