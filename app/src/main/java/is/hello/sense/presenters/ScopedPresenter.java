package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.util.Logger;

public abstract class ScopedPresenter<T extends Output> {

    private boolean stateRestored = false;
    protected T view;

    /**
     * @param view Bind reference to PresenterOutput
     */
    public void setView(final T view){
        this.view = view;
    }

    /**
     * Release reference to PresenterOutput
     */
    public void onDestroyView(){
        this.view = null;
    }

    /**
     * Release reference to Intercepter
     */
    public abstract void onDestroy();

    public boolean isStateRestored() {
        return stateRestored;
    }

    public void onRestoreState(@NonNull final Bundle savedState) {
        logEvent("onRestoreState(" + savedState + ")");
        stateRestored = true;
    }

    public @Nullable
    Bundle onSaveState() {
        logEvent("onSaveState()");
        return null;
    }

    public @NonNull String getSavedStateKey() {
        return getClass().getSimpleName() + "#instanceState";
    }

    //region Logging

    protected void logEvent(@NonNull String event) {
        Logger.debug("scopedPresenters", getClass().getSimpleName() + ": " + event);
    }

    //endregion
}
