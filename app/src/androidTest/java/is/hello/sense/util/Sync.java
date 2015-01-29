package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.BlockingObservable;

public final class Sync<T> {
    private final BlockingObservable<T> observable;

    //region Creation

    public static <T> Sync<T> of(@NonNull Observable<T> source) {
        if (source instanceof PresenterSubject) {
            throw new IllegalArgumentException("of(Observable) cannot be used with PresenterSubject!");
        }

        return new Sync<>(source);
    }

    public static <T> Sync<T> of(@NonNull PresenterSubject<T> source, int limit) {
        return new Sync<>(source.take(limit));
    }

    private Sync(@NonNull Observable<T> source) {
        this.observable = source.toBlocking();
    }

    //endregion


    //region Binding

    public void forAll(@NonNull Action1<T> onValue,
                       @Nullable Action1<Throwable> onError,
                       @Nullable Action0 onComplete) {
        try {
            observable.forEach(onValue);
            if (onComplete != null) {
                onComplete.call();
            }
        } catch (Throwable e) {
            if (onError != null) {
                onError.call(e);
            } else {
                throw e;
            }
        }
    }

    public void forAll(@NonNull Action1<T> onValue,
                       @Nullable Action1<Throwable> onError) {
        forAll(onValue, onError, null);
    }

    public void forAll(@NonNull Action1<T> onValue) {
        forAll(onValue, null, null);
    }

    //endregion
}
