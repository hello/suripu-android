package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

import is.hello.buruberi.util.Rx;
import is.hello.sense.graph.Scope;
import rx.Subscriber;
import rx.Subscription;

public abstract class ScopedValuePresenter<T extends Serializable> extends ValuePresenter<T> {
    private @Nullable Scope scope;
    private @Nullable Subscription scopeUpdateSubscription;

    protected String getScopeValueKey() {
        return getClass().getName();
    }

    public boolean bindScope(@NonNull Scope scope) {
        if (this.scope == scope) {
            return false;
        }

        Object value = scope.retrieveValue(getScopeValueKey());
        boolean hasValue = (value != null && !subject.hasValue());
        if (hasValue) {
            logEvent("bindScope(" + scope + ")");

            //noinspection unchecked
            subject.onNext((T) value);
        }

        // A normal subscribe call would drop the subscription onError, that's not what we want.
        this.scopeUpdateSubscription = subject.observeOn(Rx.mainThreadScheduler()).unsafeSubscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                // Do nothing
            }

            @Override
            public void onNext(T newValue) {
                if (isUnsubscribed()) {
                    return;
                }

                scope.storeValue(getScopeValueKey(), newValue);
            }
        });

        return hasValue;
    }

    public void unbindScope() {
        if (scopeUpdateSubscription != null) {
            logEvent("unbindScope()");

            scopeUpdateSubscription.unsubscribe();
            this.scopeUpdateSubscription = null;
        }

        this.scope = null;
    }
}
