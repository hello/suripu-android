package is.hello.sense.graph;

import junit.framework.TestCase;

import is.hello.sense.util.Sync;
import rx.Observable;
import rx.subjects.ReplaySubject;

import static is.hello.sense.AssertExtensions.assertThrows;

public class PendingObservablesTests extends TestCase {
    private static final String TOKEN = "TOKEN";

    public void testCoalesces() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        Observable<String> neverEmits = Observable.create(s -> {});
        Observable<String> firstInstance = test.bind(TOKEN, neverEmits);
        Observable<String> secondInstance = test.bind(TOKEN, neverEmits);
        assertSame(firstInstance, secondInstance);
    }

    public void testMirrorsValue() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        ReplaySubject<String> source = ReplaySubject.createWithSize(1);
        Observable<String> mirror = test.bind(TOKEN, source);

        source.onNext("test");
        source.onCompleted();

        String value = Sync.last(mirror);
        assertNotNull(value);
        assertEquals("test", value);
    }

    public void testMirrorsError() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        ReplaySubject<String> source = ReplaySubject.createWithSize(1);
        Observable<String> mirror = test.bind(TOKEN, source);

        source.onError(new Throwable("whatever"));
        assertThrows(() -> {
            Sync.last(mirror);
        });
    }

    public void testClears() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        ReplaySubject<String> source = ReplaySubject.createWithSize(1);
        Observable<String> mirror = test.bind(TOKEN, source);
        assertTrue(test.hasPending(TOKEN));

        source.onNext("ignored");
        source.onCompleted();
        Sync.last(mirror);

        assertFalse(test.hasPending(TOKEN));
    }
}
