package is.hello.sense.graph;

import org.junit.Test;

import is.hello.sense.util.Sync;
import rx.Observable;
import rx.subjects.ReplaySubject;

import static is.hello.sense.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PendingObservablesTests {
    private static final String TOKEN = "TOKEN";

    @Test
    public void coalesces() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        Observable<String> neverEmits = Observable.create(s -> {});
        Observable<String> firstInstance = test.bind(TOKEN, neverEmits);
        Observable<String> secondInstance = test.bind(TOKEN, neverEmits);
        assertSame(firstInstance, secondInstance);
    }

    @Test
    public void mirrorsValue() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        ReplaySubject<String> source = ReplaySubject.createWithSize(1);
        Observable<String> mirror = test.bind(TOKEN, source);

        source.onNext("test");
        source.onCompleted();

        String value = Sync.last(mirror);
        assertNotNull(value);
        assertEquals("test", value);
    }

    @Test
    public void mirrorsError() throws Exception {
        PendingObservables<String> test = new PendingObservables<>();
        ReplaySubject<String> source = ReplaySubject.createWithSize(1);
        Observable<String> mirror = test.bind(TOKEN, source);

        source.onError(new Throwable("whatever"));
        assertThrows(() -> {
            Sync.last(mirror);
        });
    }

    @Test
    public void clears() throws Exception {
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
