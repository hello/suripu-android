package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

@SuppressLint("CommitPrefEdits")
public class PreferencesPresenterTests extends InjectionTestCase {
    private static final String TEST_KEY = "test_key";

    @Inject PreferencesPresenter presenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        presenter.edit()
                 .remove(TEST_KEY)
                 .commit();
    }

    public void testObservableString() throws Exception {
        Observable<String> test = presenter.observableString(TEST_KEY, "placeholder");
        SyncObserver<String> testObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver.await();

        assertNull(testObserver.getError());
        assertNotNull(testObserver.getSingle());
        assertEquals("placeholder", testObserver.getSingle());


        presenter.edit()
                 .putString(TEST_KEY, "actual value")
                 .commit();

        SyncObserver<String> testObserver2 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver2.await();

        assertNull(testObserver2.getError());
        assertNotNull(testObserver2.getSingle());
        assertEquals("actual value", testObserver2.getSingle());
    }

    public void testObservableBoolean() throws Exception {
        Observable<Boolean> test = presenter.observableBoolean(TEST_KEY, true);
        SyncObserver<Boolean> testObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver.await();

        assertNull(testObserver.getError());
        assertNotNull(testObserver.getSingle());
        assertEquals(true, (boolean) testObserver.getSingle());


        presenter.edit()
                 .putBoolean(TEST_KEY, false)
                 .commit();

        SyncObserver<Boolean> testObserver2 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver2.await();

        assertNull(testObserver2.getError());
        assertNotNull(testObserver2.getSingle());
        assertEquals(false, (boolean) testObserver2.getSingle());
    }
}
