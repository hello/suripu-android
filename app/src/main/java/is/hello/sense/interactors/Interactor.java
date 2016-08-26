package is.hello.sense.interactors;

import android.content.ComponentCallbacks2;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.ui.common.StateSaveable;
import is.hello.sense.util.Logger;

public abstract class Interactor implements StateSaveable {
    /**
     * The first level at which Interactors will begin
     * quietly forgetting data which can be recreated.
     */
    public static final int BASE_TRIM_LEVEL = ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;

    private boolean stateRestored = false;
    protected boolean forgotDataForLowMemory = false;

    //region Lifecycle

    public Interactor() {
    }

    /**
     * Informs the interactor that its containing Fragment/Activity has been destroyed.
     * <p/>
     * This callback is intended for use with non-singleton interactors. It will be
     * invoked automatically by {@see is.hello.sense.graph.interactors.InteractorContainer}s.
     */
    public void onContainerDestroyed() {
        logEvent("onContainerDestroyed()");
    }

    /**
     * Informs the interactor that its containing Fragment/Activity has been resumed.
     * <p/>
     * This callback is intended to be used to undo the effects of {@see onTrimMemory} being called.
     */
    public void onContainerResumed() {
        logEvent("onContainerResumed()");
        if (forgotDataForLowMemory) {
            onReloadForgottenData();
        }

        this.forgotDataForLowMemory = false;
    }

    /**
     * Informs the interactor that it should trim resources in response to different conditions.
     * <p/>
     * {@see onContainerResumed} is the intended point for the presenter to rebuild
     * any resources destroyed in response to memory pressure.
     * @see android.content.ComponentCallbacks2
     */
    public void onTrimMemory(final int level) {
        logEvent("onTrimMemory(" + level + ")");
        if (level >= BASE_TRIM_LEVEL) {
            this.forgotDataForLowMemory = onForgetDataForLowMemory();
        }
    }

    /**
     * Convenience method that informs interactor subclasses when to reload forgotten data.
     */
    protected void onReloadForgottenData() {

    }

    /**
     * Convenience method called when the interactor receives a background memory warning.
     * @return  true if the data was forgotten; false otherwise. Default value is false.
     */
    protected boolean onForgetDataForLowMemory() {
        return false;
    }

    //endregion


    //region State Restoration
    @Override
    public boolean isStateRestored() {
        return stateRestored;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        logEvent("onRestoreState(" + savedState + ")");
        this.stateRestored = true;
    }

    @Override
    public @Nullable Bundle onSaveState() {
        logEvent("onSaveState()");
        return null;
    }

    @Override
    public @NonNull String getSavedStateKey() {
        return getClass().getSimpleName() + "#instanceState";
    }

    //endregion


    //region Logging

    protected void logEvent(@NonNull final String event) {
        Logger.debug("is/hello/sense/interactors", getClass().getSimpleName() + ": " + event);
    }

    //endregion
}
