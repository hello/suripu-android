package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import is.hello.sense.graph.Scope;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.interactors.ScopedValueInteractor.BindResult;
import is.hello.sense.util.Sync;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ScopedValueInteractorTests extends SenseTestCase {
    @Test
    public void bindScope() {
        final Scope scope = new TestScope();
        final CounterInteractor originalPresenter = new CounterInteractor();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));

        final CounterInteractor nextPresenter = new CounterInteractor();
        assertThat(nextPresenter.bindScope(scope), is(BindResult.TOOK_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));
    }

    @Test
    public void bindScopePropagation() {
        final Scope scope = new TestScope();
        final CounterInteractor originalPresenter = new CounterInteractor();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));

        assertThat(Sync.wrapAfter(originalPresenter::update,
                                  originalPresenter.latest()).last(), is(equalTo(1)));

        final CounterInteractor futurePresenter = new CounterInteractor();
        assertThat(futurePresenter.bindScope(scope), is(BindResult.TOOK_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(1)));
    }

    @Test
    public void bindScopeIdempotent() {
        final Scope scope = new TestScope();
        final CounterInteractor originalPresenter = new CounterInteractor();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));
        assertThat(originalPresenter.bindScope(scope), is(BindResult.UNCHANGED));
    }

    @Test
    public void unbindScope() {
        final Scope scope = new TestScope();
        final CounterInteractor originalPresenter = new CounterInteractor();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));

        originalPresenter.unbindScope();

        // Future updates _should not_ propagate.
        assertThat(Sync.wrapAfter(originalPresenter::update,
                                  originalPresenter.latest()).last(), is(equalTo(1)));

        final CounterInteractor nextPresenter = new CounterInteractor();
        assertThat(nextPresenter.bindScope(scope), is(BindResult.TOOK_VALUE));
        assertThat(Sync.last(nextPresenter.latest()), is(equalTo(0)));
    }

    static class TestScope implements Scope {
        private Map<String, Object> storage = new HashMap<>();

        @NonNull
        @Override
        public Scheduler getScopeScheduler() {
            return Schedulers.immediate();
        }

        @Override
        public void storeValue(@NonNull String key, @Nullable Object value) {
            if (value != null) {
                storage.put(key, value);
            } else {
                storage.remove(key);
            }
        }

        @Nullable
        @Override
        public Object retrieveValue(@NonNull String key) {
            return storage.get(key);
        }
    }

    static class CounterInteractor extends ScopedValueInteractor<Number> {
        private int count = 0;

        @Override
        protected boolean isDataDisposable() {
            return true;
        }

        @Override
        protected boolean canUpdate() {
            return true;
        }

        @Override
        protected Observable<Number> provideUpdateObservable() {
            return Observable.just(this.count++);
        }
    }
}
