package is.hello.sense.graph;

import android.os.Bundle;

import junit.framework.TestCase;

import is.hello.sense.util.SyncObserver;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
public class PresenterSubjectTests extends TestCase {
    public void testSimplePropagation() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, subject);
        subject.onNext(42);
        observer.await();

        assertNull(observer.getError());
        assertEquals(42, (int) observer.getSingle());

        observer.reset().subscribeTo(subject);

        subject.onError(new IllegalStateException());
        observer.await();

        assertNotNull(observer.getError());
        assertEquals(IllegalStateException.class, observer.getError().getClass());
    }

    public void testHistory() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onNext(42);

        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, subject);
        observer.await();

        assertNull(observer.getError());
        assertEquals(42, (int) observer.getSingle());


        observer.reset().subscribeTo(subject);

        subject.onError(new IllegalStateException());
        observer.await();

        assertNotNull(observer.getError());
        assertEquals(IllegalStateException.class, observer.getError().getClass());
    }

    public void testResumesAfterError() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onNext(42);

        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, subject);
        observer.await();

        assertNull(observer.getError());
        assertEquals(42, (int) observer.getSingle());

        observer.reset();

        subject.onError(new IllegalStateException());
        observer.subscribeTo(subject).await();

        assertNotNull(observer.getError());
        assertEquals(IllegalStateException.class, observer.getError().getClass());

        observer.reset();

        subject.onNext(9000);
        observer.subscribeTo(subject).await();

        assertEquals(9000, (int) observer.getLast());
    }

    public void testNonCompletion() throws Exception {
        PresenterSubject<Integer> subject = PresenterSubject.create();
        subject.onCompleted();
        subject.onNext(42);

        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, subject);
        observer.await();

        assertNull(observer.getError());
        assertEquals(42, (int) observer.getSingle());
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
