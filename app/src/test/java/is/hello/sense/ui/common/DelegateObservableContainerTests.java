package is.hello.sense.ui.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SenseTestCase;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DelegateObservableContainerTests extends SenseTestCase {
    private final FakeTarget fakeTarget = new FakeTarget();
    private DelegateObservableContainer<FakeTarget> container;

    @Before
    public void setUp() {
        this.container = new DelegateObservableContainer<>(Schedulers.immediate(),
                                                           fakeTarget,
                                                           FakeTarget::isValid);
    }

    @After
    public void tearDown() {
        fakeTarget.reset();
    }


    @Test
    public void clearSubscriptions() {
        final Observable<Integer> empty = Observable.create(s -> {});
        final Subscription subscription1 = container.track(empty.subscribe());
        final Subscription subscription2 = container.track(empty.subscribe());
        final Subscription subscription3 = container.track(empty.subscribe());

        assertThat(container.hasSubscriptions(), is(true));
        assertThat(subscription1.isUnsubscribed(), is(false));
        assertThat(subscription2.isUnsubscribed(), is(false));
        assertThat(subscription3.isUnsubscribed(), is(false));

        container.clearSubscriptions();

        assertThat(container.hasSubscriptions(), is(false));
        assertThat(subscription1.isUnsubscribed(), is(true));
        assertThat(subscription2.isUnsubscribed(), is(true));
        assertThat(subscription3.isUnsubscribed(), is(true));
    }

    @Test
    public void bind() {
        final ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);

        final AtomicInteger accumulator = new AtomicInteger();
        container.bind(source).subscribe(accumulator::addAndGet);

        source.onNext(1);
        source.onNext(3);
        assertThat(accumulator.get(), is(equalTo(4)));

        fakeTarget.destroy();
        source.onNext(9);
        source.onNext(32);
        assertThat(accumulator.get(), is(equalTo(4)));

        fakeTarget.reset();
        source.onNext(4);

        assertThat(accumulator.get(), is(equalTo(4)));
    }

    @Test
    public void subscribe() {
        final PresenterSubject<Integer> source = PresenterSubject.create();

        final AtomicInteger accumulator = new AtomicInteger();
        final List<Throwable> errors = new ArrayList<>();
        container.subscribe(source, accumulator::addAndGet, errors::add);

        source.onNext(3);
        assertThat(accumulator.get(), is(equalTo(3)));
        assertThat(errors.size(), is(equalTo(0)));

        source.onError(new Throwable("Oh no!"));
        assertThat(accumulator.get(), is(equalTo(3)));
        assertThat(errors.size(), is(equalTo(1)));

        source.onNext(2);
        assertThat(accumulator.get(), is(equalTo(5)));
        assertThat(errors.size(), is(equalTo(1)));

        source.onError(new Throwable("Oh no again!"));
        assertThat(accumulator.get(), is(equalTo(5)));
        assertThat(errors.size(), is(equalTo(2)));

        source.onNext(5);
        assertThat(accumulator.get(), is(equalTo(10)));
        assertThat(errors.size(), is(equalTo(2)));
    }


    static class FakeTarget {
        private boolean destroyed = false;

        void destroy() {
            this.destroyed = true;
        }

        void reset() {
            this.destroyed = false;
        }

        boolean isValid() {
            return !destroyed;
        }
    }
}
