package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import is.hello.sense.graph.Scope;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.graph.presenters.ScopedValuePresenter.BindResult;
import is.hello.sense.util.Sync;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ScopedValuePresenterTests extends SenseTestCase {
    @Test
    public void bindScope() {
        final Scope scope = new TestScope();
        final CounterPresenter originalPresenter = new CounterPresenter();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));

        final CounterPresenter nextPresenter = new CounterPresenter();
        assertThat(nextPresenter.bindScope(scope), is(BindResult.TOOK_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));
    }

    @Test
    public void bindScopePropagation() {
        final Scope scope = new TestScope();
        final CounterPresenter originalPresenter = new CounterPresenter();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));

        assertThat(Sync.wrapAfter(originalPresenter::update,
                                  originalPresenter.latest()).last(), is(equalTo(1)));

        final CounterPresenter futurePresenter = new CounterPresenter();
        assertThat(futurePresenter.bindScope(scope), is(BindResult.TOOK_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(1)));
    }

    @Test
    public void bindScopeIdempotent() {
        final Scope scope = new TestScope();
        final CounterPresenter originalPresenter = new CounterPresenter();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));
        assertThat(originalPresenter.bindScope(scope), is(BindResult.UNCHANGED));
    }

    @Test
    public void unbindScope() {
        final Scope scope = new TestScope();
        final CounterPresenter originalPresenter = new CounterPresenter();
        assertThat(originalPresenter.bindScope(scope), is(BindResult.WAITING_FOR_VALUE));
        assertThat(Sync.last(originalPresenter.latest()), is(equalTo(0)));

        originalPresenter.unbindScope();

        // Future updates _should not_ propagate.
        assertThat(Sync.wrapAfter(originalPresenter::update,
                                  originalPresenter.latest()).last(), is(equalTo(1)));

        final CounterPresenter nextPresenter = new CounterPresenter();
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

    static class CounterPresenter extends ScopedValuePresenter<Number> {
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
