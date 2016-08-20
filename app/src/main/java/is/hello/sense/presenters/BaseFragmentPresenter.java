package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.SenseApplication;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.presenters.outputs.BaseOutput;

public class BaseFragmentPresenter<T extends BaseOutput> extends BasePresenter<T> {

    public BaseFragmentPresenter(){
        //todo erase after Simon updates
        SenseApplication.getInstance().inject(this);
    }

    public void restoreState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        interactorContainer.onRestoreState(savedInstanceState);
    }

    public void onResume() {
        stateSafeExecutor.executePendingForResume();
        interactorContainer.onContainerResumed();
    }

    public void onSaveState(@NonNull final Bundle outState) {
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
}
