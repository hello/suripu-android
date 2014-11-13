package is.hello.sense.graph;

import is.hello.sense.util.Logger;
import rx.Observer;
import rx.Subscriber;

/**
 * Wraps an {@see rx.Observer} object and guards against calls after
 * a subscription has been {@see rx.Subscription#unsubscribe()}d.
 * <p />
 * This class exists because the actual behavior of SafeSubscriber appears
 * to differ from the documented behavior. That is to say, calls to onNext,
 * etc were still being propagated, even after a subscription was unsubscribed.
 * This is a simple shim to work around the issue.
 */
public final class SafeObserverWrapper<T> extends Subscriber<T> {
    private final Observer<T> target;

    public SafeObserverWrapper(Observer<T> target) {
        super();

        this.target = target;
    }

    @Override
    public void onCompleted() {
        if (isUnsubscribed()) {
            Logger.debug(SafeObserverWrapper.class.getSimpleName(), "onCompleted called after subscription destroyed");
            return;
        }

        target.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        if (isUnsubscribed()) {
            Logger.debug(SafeObserverWrapper.class.getSimpleName(), "onError called after subscription destroyed");
            return;
        }

        target.onError(e);
    }

    @Override
    public void onNext(T t) {
        if (isUnsubscribed()) {
            Logger.debug(SafeObserverWrapper.class.getSimpleName(), "onNext called after subscription destroyed");
            return;
        }

        target.onNext(t);
    }
}
