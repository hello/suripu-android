package is.hello.sense.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.PresenterContainer;

public class InjectionDialogFragment extends SenseDialogFragment implements ObservableContainer {
    protected final PresenterContainer presenterContainer = new PresenterContainer();

    public InjectionDialogFragment() {
        SenseApplication.getInstance().inject(this);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            presenterContainer.onRestoreState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        presenterContainer.onSaveState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenterContainer.onContainerResumed();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        observableContainer.clearSubscriptions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        presenterContainer.onContainerDestroyed();
    }


    public void addPresenter(@NonNull Presenter presenter) {
        presenterContainer.addPresenter(presenter);
    }
}
