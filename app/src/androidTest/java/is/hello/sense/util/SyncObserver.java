package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;

public final class SyncObserver<T> implements Observer<T> {
    public static final long STANDARD_TIMEOUT = 750;

    private final WaitingFor waitingFor;
    private CountDownLatch latch = new CountDownLatch(1);

    private Throwable error;
    private ArrayList<T> results = new ArrayList<>();

    private int ignoreCount = 0;
    private Subscription subscription;

    private SyncObserver(@NonNull WaitingFor waitingFor) {
        this.waitingFor = waitingFor;
    }

    public static <T> SyncObserver<T> subscribe(@NonNull WaitingFor waitingFor, @NonNull Observable<T> observable) {
        return new SyncObserver<T>(waitingFor).subscribeTo(observable);
    }


    @Override
    public void onCompleted() {
        if (waitingFor == WaitingFor.COMPLETED && (ignoreCount-- <= 0))
            latch.countDown();
    }

    @Override
    public void onError(Throwable e) {
        this.error = e;
        latch.countDown();
    }

    @Override
    public void onNext(T t) {
        results.add(t);
        if (waitingFor == WaitingFor.NEXT && (ignoreCount-- <= 0))
            latch.countDown();
    }


    public SyncObserver<T> subscribeTo(@NonNull Observable<T> observable) {
        this.subscription = observable.subscribeOn(Schedulers.computation()).subscribe(this);
        return this;
    }

    public SyncObserver<T> ignore(int amount) {
        this.ignoreCount = amount;
        return this;
    }

    public SyncObserver<T> reset() {
        this.latch = new CountDownLatch(1);
        this.error = null;
        this.ignoreCount = 0;
        results.clear();

        return this;
    }


    public boolean await() throws InterruptedException {
        return await(STANDARD_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean result = latch.await(timeout, unit);
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            this.subscription = null;
        }
        return result;
    }


    public @Nullable Throwable getError() {
        return error;
    }

    public @NonNull ArrayList<T> getResults() {
        return results;
    }

    public @Nullable T getSingle() {
        if (results.size() > 1)
            throw new IllegalStateException("getSingle called with multiple results");
        else if (results.isEmpty())
            return null;
        else
            return results.get(0);
    }

    public @Nullable T getLast() {
        if (results.isEmpty())
            return null;
        else
            return results.get(results.size() - 1);
    }


    public static enum WaitingFor {
        NEXT,
        COMPLETED,
    }
}
