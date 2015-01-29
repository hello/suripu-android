package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.BlockingObservable;

public class Sync<T> {
    private final BlockingObservable<T> observable;

    public static <T> Sync<T> of(@NonNull Observable<T> source) {
        return new Sync<>(source);
    }

    public Sync(@NonNull Observable<T> source) {
        this.observable = source.toBlocking();
    }

    public void forNext(@NonNull Action1<T> onValue,
                        @NonNull Action1<Throwable> onError) {

    }

    public void forCompletion(@NonNull Action1<T> onValue,
                              @NonNull Action1<Throwable> onError,
                              @Nullable Action0 onCompletion) {

    }

    public void forCompletion(@NonNull Action1<T> onValue,
                              @NonNull Action1<Throwable> onError) {
        forCompletion(onValue, onError, null);
    }
}
