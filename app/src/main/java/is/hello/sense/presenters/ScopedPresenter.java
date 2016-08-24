package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.util.Logger;

public abstract class ScopedPresenter<T extends BaseOutput> extends BaseFragmentPresenter<T> {

    private boolean stateRestored = false;

    //region StateSaveable

    @Override
    public boolean isStateRestored() {
        return stateRestored;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        logEvent("onRestoreState(" + savedState + ")");
        stateRestored = true;
        interactorContainer.onRestoreState(savedState);
    }

    @Override
    @Nullable
    public Bundle onSaveState() {
        logEvent("onSaveState()");
        return null;
    }

    @Override
    @NonNull
    public String getSavedStateKey() {
        return getClass().getSimpleName() + "#instanceState";
    }

    //endregion

    //region Logging

    protected void logEvent(@NonNull final String event) {
        Logger.debug("scopedPresenters", getClass().getSimpleName() + ": " + event);
    }


    //endregion
}
