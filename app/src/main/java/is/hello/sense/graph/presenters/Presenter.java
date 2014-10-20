package is.hello.sense.graph.presenters;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.util.Logger;

public abstract class Presenter {
    private boolean stateRestored = false;

    //region Lifecycle

    public Presenter() {
    }

    /**
     * Informs the presenter that its containing Fragment/Activity has been destroyed.
     * <p/>
     * This callback is intended for use with non-singleton presenters. It will be
     * invoked automatically by {@see is.hello.sense.graph.presenters.PresenterContainer}s.
     */
    public void onContainerDestroyed() {
        logEvent("onContainerDestroyed()");
    }

    //endregion


    //region State Restoration

    public boolean isStateRestored() {
        return stateRestored;
    }

    public void onRestoreState(@NonNull Parcelable savedState) {
        logEvent("onRestoreState(" + savedState + ")");
        this.stateRestored = true;
    }

    public @Nullable Parcelable onSaveState() {
        logEvent("onSaveState()");
        return null;
    }

    public @NonNull String getSavedStateKey() {
        return getClass().getSimpleName() + "#instanceState";
    }

    //endregion


    //region Logging

    protected void logEvent(@NonNull String event) {
        Logger.debug("presenters", getClass().getSimpleName() + ": " + event);
    }

    //endregion
}
