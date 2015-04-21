package is.hello.sense.graph;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.subjects.AsyncSubject;

/**
 * Tracks and mirrors Observables which cannot be safely subscribed to more than once.
 * @param <T>   The type of token to use for coalescing.
 */
public final class PendingObservables<T> {
    private final Map<T, AsyncSubject<?>> pending = new HashMap<>();

    /**
     * Returns whether or not there is pending Observable matching a given token.
     */
    public boolean hasPending(@NonNull T token) {
        synchronized (pending) {
            return pending.containsKey(token);
        }
    }

    /**
     * Checks if an existing Observable with a given token is contained in the receiver;
     * if not, creates a mirror for the given Observable, then caches and returns it.
     * @param token     The token to use for tracking the Observable.
     * @param provider  The functor that will generate the Observable if needed.
     */
    public <U> Observable<U> bind(@NonNull T token, @NonNull Func0<Observable<U>> provider) {
        synchronized (pending) {
            //noinspection unchecked
            AsyncSubject<U> existing = (AsyncSubject<U>) pending.get(token);
            if (existing != null) {
                return existing;
            } else {
                AsyncSubject<U> mirror = AsyncSubject.create();
                mirror.subscribe(new Subscriber<U>() {
                    @Override
                    public void onCompleted() {
                        synchronized (pending) {
                            pending.remove(token);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        onCompleted();
                    }

                    @Override
                    public void onNext(U ignored) {}
                });
                pending.put(token, mirror);

                Observable<U> source = provider.call();
                source.subscribe(mirror);
                return mirror;
            }
        }
    }

    /**
     * Same as {@link #bind(Object, rx.functions.Func0)}, but with
     * an eagerly evaluated observable reference instead of lazy.
     */
    public <U> Observable<U> bind(@NonNull T token, @NonNull Observable<U> observable) {
        return bind(token, () -> observable);
    }
}
