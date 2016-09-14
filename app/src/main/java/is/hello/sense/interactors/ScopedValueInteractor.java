package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

import is.hello.sense.graph.Scope;
import rx.Subscriber;
import rx.Subscription;

public abstract class ScopedValueInteractor<T extends Serializable> extends ValueInteractor<T> {
    private @Nullable Scope scope;
    private @Nullable Subscription scopeUpdateSubscription;

    protected String getScopeValueKey() {
        return getClass().getName();
    }

    public BindResult bindScope(@NonNull Scope scope) {
        if (this.scope == scope) {
            return BindResult.UNCHANGED;
        }

        logEvent("bindScope(" + scope + ")");

        this.scope = scope;

        final Object value = scope.retrieveValue(getScopeValueKey());
        final boolean tookValue = (value != null && !subject.hasValue());
        if (tookValue) {
            //noinspection unchecked
            subject.onNext((T) value);
        }

        if (scopeUpdateSubscription != null) {
            scopeUpdateSubscription.unsubscribe();
        }

        // A normal subscribe call would drop the subscription onError, that's not what we want.
        this.scopeUpdateSubscription =
                subject.observeOn(scope.getScopeScheduler())
                       .unsafeSubscribe(new Subscriber<T>() {
                           @Override
                           public void onCompleted() {
                               // Do nothing.
                           }

                           @Override
                           public void onError(Throwable e) {
                               // Do nothing.
                           }

                           @Override
                           public void onNext(T newValue) {
                               if (isUnsubscribed()) {
                                   return;
                               }

                               scope.storeValue(getScopeValueKey(), newValue);
                           }
                       });

        return tookValue ? BindResult.TOOK_VALUE : BindResult.WAITING_FOR_VALUE;
    }

    public void unbindScope() {
        if (scopeUpdateSubscription != null) {
            logEvent("unbindScope()");

            scopeUpdateSubscription.unsubscribe();
            this.scopeUpdateSubscription = null;
        }

        this.scope = null;
    }

    public enum BindResult {
        UNCHANGED,
        TOOK_VALUE,
        WAITING_FOR_VALUE,
    }
}
