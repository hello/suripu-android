package is.hello.sense.util;

import android.support.annotation.NonNull;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

public class UnsafeOperatorObserveOn<T> implements Observable.Operator<T, T> {
    private final Scheduler scheduler;

    public UnsafeOperatorObserveOn(@NonNull Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        return new UnsafeObserveOnSubscriber<>(child, scheduler);
    }

    private static class UnsafeObserveOnSubscriber<T> extends Subscriber<T> {
        private final Subscriber<T> child;
        private final Scheduler.Worker worker;

        private UnsafeObserveOnSubscriber(@NonNull Subscriber<T> child, @NonNull Scheduler scheduler) {
            super(child);

            this.child = child;
            this.worker = scheduler.createWorker();
        }

        @Override
        public void onCompleted() {
            worker.schedule(child::onCompleted);
        }

        @Override
        public void onError(Throwable e) {
            worker.schedule(() -> child.onError(e));
        }

        @Override
        public void onNext(T value) {
            worker.schedule(() -> child.onNext(value));
        }
    }
}
