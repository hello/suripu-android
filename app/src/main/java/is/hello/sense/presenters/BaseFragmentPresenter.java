package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.InteractorContainer;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.StateSaveable;

/**
 * Contains {@link InteractorContainer}
 * @param <T> see {@link BasePresenter}
 */
public abstract class BaseFragmentPresenter<T extends BaseOutput> extends BasePresenter<T>
        implements StateSaveable {
    protected final InteractorContainer interactorContainer = new InteractorContainer();

    public BaseFragmentPresenter(){

    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedInstanceState) {
        interactorContainer.onRestoreState(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        interactorContainer.onContainerDestroyed();
    }

    public void onResume() {
        stateSafeExecutor.executePendingForResume();
        interactorContainer.onContainerResumed();
    }

    public void onSaveInteractorState(@NonNull final Bundle outState) {
        interactorContainer.onSaveState(outState);
    }

    public void onTrimMemory(final int level) {
        interactorContainer.onTrimMemory(level);
    }

    public void addInteractor(@NonNull final Interactor interactor) {
        interactorContainer.addInteractor(interactor);
    }

    public void execute(@NonNull final Runnable runnable) {
        stateSafeExecutor.execute(runnable);
    }

    public Runnable bind(@NonNull final Runnable runnable) {
        return stateSafeExecutor.bind(runnable);
    }
}
