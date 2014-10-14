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

    public void testObservableInteger() throws Exception {
        Observable<Integer> test = presenter.observableInteger(TEST_KEY, 10);
        SyncObserver<Integer> testObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver.await();

        assertNull(testObserver.getError());
        assertNotNull(testObserver.getSingle());
        assertEquals(10, (int) testObserver.getSingle());


        presenter.edit()
                .putInt(TEST_KEY, 99)
                .commit();

        SyncObserver<Integer> testObserver2 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver2.await();

        assertNull(testObserver2.getError());
        assertNotNull(testObserver2.getSingle());
        assertEquals(99, (int) testObserver2.getSingle());
    }

    public void testObservableLong() throws Exception {
        Observable<Long> test = presenter.observableLong(TEST_KEY, 10);
        SyncObserver<Long> testObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver.await();

        assertNull(testObserver.getError());
        assertNotNull(testObserver.getSingle());
        assertEquals(10, (long) testObserver.getSingle());


        presenter.edit()
                .putLong(TEST_KEY, 99)
                .commit();

        SyncObserver<Long> testObserver2 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver2.await();

        assertNull(testObserver2.getError());
        assertNotNull(testObserver2.getSingle());
        assertEquals(99, (long) testObserver2.getSingle());
    }

    public void testObservableFloat() throws Exception {
        Observable<Float> test = presenter.observableFloat(TEST_KEY, 4f);
        SyncObserver<Float> testObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver.await();

        assertNull(testObserver.getError());
        assertNotNull(testObserver.getSingle());
        assertEquals(4f, testObserver.getSingle());


        presenter.edit()
                .putFloat(TEST_KEY, 8f)
                .commit();

        SyncObserver<Float> testObserver2 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, test);
        testObserver2.await();

        assertNull(testObserver2.getError());
        assertNotNull(testObserver2.getSingle());
        assertEquals(8f, testObserver2.getSingle());
    }
}
