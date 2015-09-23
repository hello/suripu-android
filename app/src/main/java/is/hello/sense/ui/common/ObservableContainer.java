package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

/**
 * The contract implemented by all <code>Injection</code> classes
 * for binding and subscribing to observable objects.
 */
public interface ObservableContainer {
    /**
     * Binds a given observable to the lifecycle of the container, returning a new observable.
     */
    @NonNull <T> Observable<T> bind(@NonNull Observable<T> toBind);

    /**
     * Returns whether or not the observable container has any tracked subscriptions.
     */
    boolean hasSubscriptions();

    /**
     * Binds a given subscription's lifecycle to the container.
     * <p/>
     * The given subscription will automatically be destroyed along with the container.
     */
    @NonNull Subscription track(@NonNull Subscription subscription);

    /**
     * Subscribes to a given observable, automatically binding the
     * subscription's destruction to the lifecycle of the container.
     */
    @NonNull <T> Subscription subscribe(@NonNull Observable<T> toSubscribe,
                                        @NonNull Action1<? super T> onNext,
                                        @NonNull Action1<Throwable> onError);

    /**
     * Binds a given observable to the container's lifecycle, and then creates
     * a subscription bound to the lifecycle of the container.
     */
    @NonNull <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe,
                                               @NonNull Action1<? super T> onNext,
                                               @NonNull Action1<Throwable> onError);
}
