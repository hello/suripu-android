package is.hello.sense.graph;

import android.os.Bundle;

import org.junit.Test;

import is.hello.sense.util.Sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
public class PresenterSubjectTests extends SenseTestCase {
    @Test
    public void simplePropagation() throws Exception {
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

    @Test
    public void resumesAfterError() throws Exception {
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

    @Test
    public void nonCompletion() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onCompleted();
        subject.onNext(42);

        int value = Sync.next(subject);
        assertEquals(42, value);
    }

    @Test
    public void serialization() throws Exception {
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
