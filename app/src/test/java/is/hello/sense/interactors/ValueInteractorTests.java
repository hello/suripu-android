package is.hello.sense.interactors;

import android.os.Bundle;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.Sync;
import rx.Observable;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ValueInteractorTests extends SenseTestCase {

    @Test
    public void overlappingUpdates() throws Exception {
       final CounterInteractor presenter = new CounterInteractor();

        presenter.update();

        final LambdaVar<Integer> lastValue = LambdaVar.of(0);
        final LambdaVar<Integer> numberOfCalls = LambdaVar.of(0);
        Sync.wrapAfter(presenter::update, presenter.value)
            .forEachAction(value -> {
                presenter.update();
                lastValue.set(value);
                numberOfCalls.getAndMutate(i -> i + 1);
            });

        assertEquals(1, numberOfCalls.get().intValue());
        assertEquals(2, lastValue.get().intValue());
    }

    @Test
    public void updateIfEmpty() throws Exception {
        final CounterInteractor presenter = new CounterInteractor();

        Sync.wrapAfter(presenter::updateIfEmpty, presenter.value)
            .assertThat(is(equalTo(1)));
        Sync.wrapAfter(presenter::updateIfEmpty, presenter.value)
            .assertThat(is(equalTo(1)));
    }

    @Test
    public void lowMemoryLogic() throws Exception {
        final CounterInteractor presenter = new CounterInteractor();

        Sync.wrapAfter(presenter::update, presenter.value)
            .assertThat(is(equalTo(1)));

        presenter.onTrimMemory(Interactor.BASE_TRIM_LEVEL);
        presenter.onContainerResumed();

        Thread.sleep(500, 0);

        Sync.wrap(presenter.value)
            .assertThat(is(equalTo(2)));
    }


    @Test
    public void onSaveStateLogic() throws Exception {
        final CounterInteractor presenter = new CounterInteractor();

        final int value = Sync.wrapAfter(presenter::update, presenter.value).last();
        final Bundle savedState = presenter.onSaveState();
        assertNotNull(savedState);
        assertTrue(savedState.containsKey(ValueInteractor.SAVED_STATE_KEY));
        assertEquals(value, savedState.getSerializable(ValueInteractor.SAVED_STATE_KEY));
    }

    @Test
    public void onRestoreStateLogic() throws Exception {
        final CounterInteractor presenter = new CounterInteractor();

        final Bundle bundle = new Bundle();
        bundle.putSerializable(ValueInteractor.SAVED_STATE_KEY, 12);

        presenter.onRestoreState(bundle);

        final int value = Sync.wrapAfter(presenter::update, presenter.value).last();
        assertEquals(12, value);
    }


    @Test
    public void latest() throws Exception {
        final CounterInteractor presenter = new CounterInteractor();
        presenter.update();

        // Tests that the latest() observable terminates.
        final int latest = Sync.last(presenter.latest());
        assertEquals(1, latest);
    }


    static class CounterInteractor extends ValueInteractor<Integer> {
        private int counter = 0;

        final InteractorSubject<Integer> value = this.subject;
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
