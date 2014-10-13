package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

public final class SyncObserver<T> implements Observer<T> {
    public static final long STANDARD_TIMEOUT = 750;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final WaitingFor waitingFor;

    private Throwable error;
    private ArrayList<T> results = new ArrayList<>();

    public SyncObserver(@NonNull WaitingFor waitingFor) {
        this.waitingFor = waitingFor;
    }

    public static <T> SyncObserver<T> subscribe(@NonNull WaitingFor waitingFor, @NonNull Observable<T> observable) {
        SyncObserver<T> observer = new SyncObserver<>(waitingFor);
        observable.subscribeOn(Schedulers.io()).subscribe(observer);
        return observer;
    }


    private void signal() {
        latch.countDown();
    }

    @Override
    public void onCompleted() {
        if (waitingFor == WaitingFor.COMPLETED)
            signal();
    }

    @Override
    public void onError(Throwable e) {
        this.error = e;
        signal();
    }

    @Override
    public void onNext(T t) {
        results.add(t);
        if (waitingFor == WaitingFor.NEXT)
            signal();
    }


    public boolean await() throws InterruptedException {
        return await(STANDARD_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }


    public @Nullable Throwable getError() {
        return error;
    }

    public @NonNull ArrayList<T> getResults() {
        return results;
    }


    public static enum WaitingFor {
        NEXT,
        COMPLETED,
    }
}
