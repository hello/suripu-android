package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

import is.hello.sense.graph.Scope;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

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

        this.scopeUpdateSubscription = subject.observeOn(AndroidSchedulers.mainThread()).subscribe(newValue -> {
            scope.storeValue(getScopeValueKey(), newValue);
        }, ignored -> {
            // Do nothing.
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
