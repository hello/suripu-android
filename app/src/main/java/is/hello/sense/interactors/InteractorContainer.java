package is.hello.sense.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains one or more child presenter objects, allowing a containing object
 * to send lifecycle events to all of its interactors with a single method call.
 *
 * @see Interactor
 */
public class InteractorContainer {
    @VisibleForTesting final List<Interactor> interactors = new ArrayList<>();

    //region Lifecycle

    /**
     * @see Interactor#onContainerDestroyed()
     */
    public void onContainerDestroyed() {
        for (Interactor interactor : interactors) {
            interactor.onContainerDestroyed();
        }
    }

    /**
     * @see Interactor#onContainerResumed()
     */
    public void onContainerResumed() {
        for (Interactor interactor : interactors) {
            interactor.onContainerResumed();
        }
    }

    /**
     * @see Interactor#onTrimMemory(int)
     */
    public void onTrimMemory(int level) {
        for (Interactor interactor : interactors) {
            interactor.onTrimMemory(level);
        }
    }

    /**
     * @see Interactor#onRestoreState(android.os.Bundle)
     */
    public void onRestoreState(@NonNull Bundle inState) {
        for (Interactor interactor : interactors) {
            if (interactor.isStateRestored()) {
                continue;
            }

            Bundle savedState = inState.getParcelable(interactor.getSavedStateKey());
            if (savedState != null) {
                interactor.onRestoreState(savedState);
            }
        }
    }

    /**
     * @see Interactor#onSaveState()
     */
    public void onSaveState(Bundle outState) {
        for (Interactor interactor : interactors) {
            Bundle savedState = interactor.onSaveState();
            if (savedState != null) {
                outState.putParcelable(interactor.getSavedStateKey(), savedState);
            }
        }
    }

    //endregion


    /**
     * Add a child interactor to the container.
     */
    public void addPresenter(@NonNull Interactor interactor) {
        interactors.add(interactor);
    }
}
