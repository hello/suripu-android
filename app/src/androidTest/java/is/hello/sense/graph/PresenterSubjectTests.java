package is.hello.sense.graph;

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

        observer.reset().subscribeTo(subject);

        subject.onError(new IllegalStateException());
        observer.await();

        assertNotNull(observer.getError());
        assertEquals(IllegalStateException.class, observer.getError().getClass());

        observer.reset().subscribeTo(subject);

        subject.onNext(9000);
        observer.await();

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
}
