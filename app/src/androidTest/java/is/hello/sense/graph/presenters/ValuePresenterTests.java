package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

public class ValuePresenterTests extends TestCase {

    public void testOverlappingUpdates() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        presenter.update();
        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.value);
        presenter.update();
        observer.await();

        assertEquals(1, observer.getResults().size());
        assertEquals(2, observer.getSingle().intValue());
    }

    public void testLowMemoryLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.value);
        presenter.update();
        observer.await();

        assertEquals(1, observer.getSingle().intValue());

        observer.reset().subscribeTo(presenter.value);

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        presenter.onContainerResumed();

        Thread.sleep(500, 0);

        observer.await();

        assertEquals(2, observer.getLast().intValue());
    }


    public void testOnSaveStateLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.value);
        presenter.update();
        observer.await();

        Parcelable savedState = presenter.onSaveState();
        assertNotNull(savedState);
        assertTrue(savedState instanceof Bundle);

        Bundle bundle = (Bundle) savedState;
        assertTrue(bundle.containsKey(ValuePresenter.SAVED_STATE_KEY));
        assertEquals(observer.getSingle(), bundle.getSerializable(ValuePresenter.SAVED_STATE_KEY));
    }

    public void testOnRestoreStateLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ValuePresenter.SAVED_STATE_KEY, 12);

        presenter.onRestoreState(bundle);

        SyncObserver<Integer> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.value);
        observer.await();

        assertEquals(observer.getSingle().intValue(), 12);
    }


    static class CounterPresenter extends ValuePresenter<Integer> {
        private int counter = 0;

        final PresenterSubject<Integer> value = this.subject;
        boolean isDataDisposable = true;
        boolean canUpdate = true;

        @Override
        protected boolean isDataDisposable() {
            return isDataDisposable;
        }

        @Override
        protected boolean canUpdate() {
            return canUpdate;
        }

        @Override
        protected Observable<Integer> provideUpdateObservable() {
            return Observable.create((Observable.OnSubscribe<Integer>) s -> {
                s.onNext(++counter);
                s.onCompleted();
            }).delay(100, TimeUnit.MILLISECONDS);
        }
    }
}
