package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Describes an object that contains one or more child presenter objects.
 * <p/>
 * In addition to the methods described in the interface,
 * the following behaviors should be implemented:
 * <ol>
 *     <li>If the container has a save/restore lifecycle, then it should call
 *     {@see Presenter.onSaveState} and {@see Presenter.onRestoreState} on
 *     each of its child presenters, saving the results into its own saved state.</li>
 *     <li>If the container has a finite lifecycle, it should inform its child presenters
 *     by calling {@see Presenter.onContainerDestroyed} when it has been destroyed.</li>
 *     <li>If the container has an onTrimMemory callback, it should inform its child
 *     presenters of this by calling {@see Presenter.onTrimMemory}.</li>
 * </ol>
 * @see is.hello.sense.ui.common.InjectionFragment
 */
public interface PresenterContainer {
    /**
     * Add a child presenter to the container.
     */
    void addPresenter(@NonNull Presenter presenter);

    /**
     * Remove a child presenter from the container, ignoring if it was not a child.
     */
    void removePresenter(@NonNull Presenter presenter);

    /**
     * Returns all of the child presenters in the container.
     * <p/>
     * The returned list should be lazily created if possible.
     */
    @NonNull List<Presenter> getPresenters();
}
