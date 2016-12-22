package is.hello.sense.ui.activities.appcompat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import is.hello.buruberi.util.Rx;
import is.hello.sense.flows.home.util.InteractorContainerProvider;
import is.hello.sense.graph.Scope;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.InteractorContainer;
import is.hello.sense.util.Logger;
import rx.Scheduler;

public abstract class ScopedInjectionActivity extends InjectionActivity
        implements Scope, InteractorContainerProvider {
    private @Nullable Map<String, Object> scopedValues;
    private final InteractorContainer interactorContainer = new InteractorContainer();

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.interactorContainer.onRestoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        this.interactorContainer.onSaveState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            interactorContainer.onContainerDestroyed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.interactorContainer.onContainerResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearValues();
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_RUNNING_MODERATE) {
            clearValues();
        }

        this.interactorContainer.onTrimMemory(level);
    }

    @NonNull
    @Override
    public Scheduler getScopeScheduler() {
        return Rx.mainThreadScheduler();
    }

    @Override
    public void storeValue(@NonNull String key, @Nullable Object value) {
        if (scopedValues == null) {
            this.scopedValues = new HashMap<>();
        }

        if (value != null) {
            scopedValues.put(key, value);
        } else {
            scopedValues.remove(key);
        }
    }

    @Nullable
    @Override
    public Object retrieveValue(@NonNull String key) {
        if (scopedValues != null) {
            return scopedValues.get(key);
        } else {
            return null;
        }
    }

    public void clearValues() {
        Logger.info(getClass().getSimpleName(), "clearValues()");
        this.scopedValues = null;
    }

    //region Interactor Container Provider

    @Override
    public void addInteractor(@NonNull final Interactor interactor){
        this.interactorContainer.addInteractor(interactor);
    }

    //endregion
}
