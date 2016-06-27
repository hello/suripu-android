package is.hello.sense.graph.presenters;

import android.os.Bundle;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.Sync;
import rx.Observable;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ValuePresenterTests extends SenseTestCase {

    @Test
    public void overlappingUpdates() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        presenter.update();

        LambdaVar<Integer> lastValue = LambdaVar.of(0);
        LambdaVar<Integer> numberOfCalls = LambdaVar.of(0);
        (Sync.wrapAfter(presenter::update, presenter.value))
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
        CounterPresenter presenter = new CounterPresenter();

        Sync.wrapAfter(presenter::updateIfEmpty, presenter.value)
            .assertThat(is(equalTo(1)));
        Sync.wrapAfter(presenter::updateIfEmpty, presenter.value)
            .assertThat(is(equalTo(1)));
    }

    @Test
    public void lowMemoryLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        Sync.wrapAfter(presenter::update, presenter.value)
            .assertThat(is(equalTo(1)));

        presenter.onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        presenter.onContainerResumed();

        Thread.sleep(500, 0);

        Sync.wrap(presenter.value)
            .assertThat(is(equalTo(2)));
    }


    @Test
    public void onSaveStateLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        int value = Sync.wrapAfter(presenter::update, presenter.value).last();
        Bundle savedState = presenter.onSaveState();
        assertNotNull(savedState);
        assertTrue(savedState.containsKey(ValuePresenter.SAVED_STATE_KEY));
        assertEquals(value, savedState.getSerializable(ValuePresenter.SAVED_STATE_KEY));
    }

    @Test
    public void onRestoreStateLogic() throws Exception {
        CounterPresenter presenter = new CounterPresenter();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ValuePresenter.SAVED_STATE_KEY, 12);

        presenter.onRestoreState(bundle);

        int value = Sync.wrapAfter(presenter::update, presenter.value).last();
        assertEquals(12, value);
    }


    @Test
    public void latest() throws Exception {
        CounterPresenter presenter = new CounterPresenter();
        presenter.update();

        // Tests that the latest() observable terminates.
        int latest = Sync.last(presenter.latest());
        assertEquals(1, latest);
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
