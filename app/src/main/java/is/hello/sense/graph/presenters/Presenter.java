package is.hello.sense.graph.presenters;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.SenseApplication;
import is.hello.sense.util.Logger;

public abstract class Presenter {
    private boolean stateRestored = false;

    public Presenter() {
        SenseApplication.getInstance().inject(this);
    }


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


    //region Primitive Methods

    public abstract void update();

    //endregion


    //region Logging

    protected void logEvent(@NonNull String event) {
        Logger.debug("presenters", getClass().getSimpleName() + ": " + event);
    }

    //endregion
}
