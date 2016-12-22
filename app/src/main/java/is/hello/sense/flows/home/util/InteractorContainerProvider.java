package is.hello.sense.flows.home.util;

import android.support.annotation.NonNull;

import is.hello.sense.interactors.Interactor;

/**
 * Manage {@link Interactor} with component lifecycle
 */

public interface InteractorContainerProvider {

    void addInteractor(@NonNull final Interactor interactor);
}
