package is.hello.sense.graph.presenters;

import android.os.Bundle;

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.Sync;
import rx.Observable;

public class ValuePresenterTests extends TestCase {

    public void testOverlappingUpdates() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        presenter.update();

        LambdaVar<Integer> lastValue = LambdaVar.of(0);
        LambdaVar<Integer> numberOfCalls = LambdaVar.of(0);
        Sync.wrapAfter(presenter::update, presenter.value)
            .forEach(value -> {
                presenter.update();
                lastValue.set(value);
                numberOfCalls.getAndMutate(i -> i + 1);
            });

        assertEquals(1, numberOfCalls.get().intValue());
        assertEquals(2, lastValue.get().intValue());
    }

    public void testLowMemoryLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        Sync.wrapAfter(presenter::update, presenter.value)
            .assertEquals(1);

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        presenter.onContainerResumed();

        Thread.sleep(500, 0);

        Sync.wrap(presenter.value).assertEquals(2);
    }


    public void testOnSaveStateLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        int value = Sync.wrapAfter(presenter::update, presenter.value).last();
        Bundle savedState = presenter.onSaveState();
        assertNotNull(savedState);
        assertTrue(savedState.containsKey(ValuePresenter.SAVED_STATE_KEY));
        assertEquals(value, savedState.getSerializable(ValuePresenter.SAVED_STATE_KEY));
    }

    public void testOnRestoreStateLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ValuePresenter.SAVED_STATE_KEY, 12);

        presenter.onRestoreState(bundle);

        int value = Sync.wrapAfter(presenter::update, presenter.value).last();
        assertEquals(12, value);
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
