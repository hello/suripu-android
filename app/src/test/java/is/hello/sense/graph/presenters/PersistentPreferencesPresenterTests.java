package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@SuppressLint("CommitPrefEdits")
public class PersistentPreferencesPresenterTests extends InjectionTestCase{
    private static final String TEST_KEY = "TEST_KEY";
    private static final String TEST_VALUE = "TEST_VALUE";
    @Inject
    PersistentPreferencesPresenter persistentPreferencesPresenter;

    @Before
    public void setUp(){
        persistentPreferencesPresenter
                .edit().clear().commit();
    }

    @Test
    public void keepsPreferencesAfterLogout(){
        persistentPreferencesPresenter
                .edit()
                .putString(TEST_KEY, TEST_VALUE)
                .commit();

        //LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ApiSessionManager.ACTION_LOGGED_OUT));
        //Robolectric.flushBackgroundThreadScheduler();

        final String value = persistentPreferencesPresenter.getString(TEST_KEY, null);

        assertNotNull(value);
        assertEquals(TEST_VALUE, value);
    }

    @Test
    public void getLastPillUpdateDateTime(){
        final String TEST_DEVICE_ID = "test_device_id";
        assertNull(persistentPreferencesPresenter.getLastPillUpdateDateTime(TEST_DEVICE_ID));
        persistentPreferencesPresenter.updateLastUpdatedDevice(TEST_DEVICE_ID);
        Robolectric.flushBackgroundThreadScheduler();

        final DateTime lastUpdated = persistentPreferencesPresenter.getLastPillUpdateDateTime(TEST_DEVICE_ID);

        assertNotNull(lastUpdated);
        assertTrue("Returned time is very close to DateTime.now",
                   Seconds.secondsBetween(DateTime.now(), lastUpdated).isLessThan(Seconds.TWO));
    }
}
