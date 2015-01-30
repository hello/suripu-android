package is.hello.sense.graph;

import android.os.Bundle;

import junit.framework.TestCase;

import is.hello.sense.util.Sync;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
public class PresenterSubjectTests extends TestCase {
    public void testSimplePropagation() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onNext(42);

        int value = Sync.next(subject);
        assertEquals(42, value);

        subject.onError(new IllegalStateException());
        try {
            Sync.next(subject);
            fail("Subject did not propagate error");
        } catch (Throwable e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }
    }

    public void testResumesAfterError() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onNext(42);

        int value1 = Sync.next(subject);
        assertEquals(42, value1);

        subject.onError(new IllegalStateException());
        try {
            Sync.next(subject);
            fail("Subject did not propagate error");
        } catch (Throwable e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }

        subject.onNext(9000);
        int value2 = Sync.next(subject);
        assertEquals(9000, value2);
    }

    public void testNonCompletion() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onCompleted();
        subject.onNext(42);

        int value = Sync.next(subject);
        assertEquals(42, value);
    }

    public void testSerialization() throws Exception {
        PresenterSubject<Integer> test1 = PresenterSubject.create();
        test1.onNext(12);

        Bundle state = new Bundle();
        assertTrue(test1.saveState("test1", state));
        assertTrue(state.containsKey("test1"));
        assertEquals(12, state.getSerializable("test1"));


        PresenterSubject<Object> test2 = PresenterSubject.create();
        test2.onNext(new Object());

        assertFalse(test2.saveState("test2", state));
        assertFalse(state.containsKey("test2"));
    }
}
